package net.minestom.server.protocol.bedrock.nethernet;

import net.minestom.server.MinecraftServer;
import net.minestom.server.network.protocol.ProtocolFamily;
import net.minestom.server.network.protocol.ProtocolVersion;
import net.minestom.server.network.protocol.ServerTransport;
import net.minestom.server.protocol.bedrock.BedrockPacketHandler;
import net.minestom.server.protocol.bedrock.BedrockProtocol;
import net.minestom.server.protocol.bedrock.nethernet.discovery.NetherNetDiscoveryCodec;
import net.minestom.server.protocol.bedrock.nethernet.discovery.NetherNetDiscoveryPacket;
import net.minestom.server.protocol.bedrock.nethernet.signaling.NetherNetSignalingMessage;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Bedrock Edition transport built on NetherNet.
 * <p>
 * NetherNet replaces RakNet with WebRTC data channels. This transport implements the parts
 * which are common to every NetherNet deployment:
 * <ul>
 *     <li>LAN discovery on the NetherNet discovery port, including the encrypted packet
 *     format;</li>
 *     <li>the WebRTC negotiation message exchange (CONNECTREQUEST / CONNECTRESPONSE /
 *     CANDIDATEADD);</li>
 *     <li>session management and SCTP segmentation of Bedrock packets.</li>
 * </ul>
 * The actual ICE/DTLS/SCTP stack is supplied by a {@link NetherNetPeerFactory}. This keeps
 * native WebRTC dependencies optional and allows a different implementation per platform.
 */
public final class NetherNetTransport implements ServerTransport {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetherNetTransport.class);

    /**
     * Value used in the discovery response to advertise the NetherNet transport.
     */
    public static final int TRANSPORT_LAYER = 2;

    private volatile boolean stop;

    private DatagramChannel channel;
    private SocketAddress socketAddress;
    private String address;
    private int port;

    private final long serverId = ThreadLocalRandom.current().nextLong();
    private final Map<String, NetherNetSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> sdpOffers = new ConcurrentHashMap<>();

    private volatile NetherNetPeerFactory peerFactory = new UnavailableNetherNetPeerFactory();
    private volatile NetherNetDiscoveryPacket.@Nullable ServerData serverData;
    private volatile @Nullable BedrockPacketHandler packetHandler;

    @Override
    public @NotNull ProtocolFamily family() {
        return ProtocolFamily.BEDROCK;
    }

    @Override
    public @NotNull ProtocolVersion protocolVersion() {
        return BedrockProtocol.VERSION;
    }

    /**
     * Sets the WebRTC implementation. Must be called before {@link #start()}.
     */
    public @NotNull NetherNetTransport peerFactory(@NotNull NetherNetPeerFactory peerFactory) {
        this.peerFactory = peerFactory;
        return this;
    }

    /**
     * Sets the data advertised in LAN discovery responses.
     */
    public @NotNull NetherNetTransport serverData(@NotNull NetherNetDiscoveryPacket.ServerData serverData) {
        this.serverData = serverData;
        return this;
    }

    /**
     * Sets the handler for Bedrock packets received on new sessions.
     */
    public @NotNull NetherNetTransport packetHandler(@Nullable BedrockPacketHandler packetHandler) {
        this.packetHandler = packetHandler;
        return this;
    }

    @Override
    public void bind(@NotNull SocketAddress address) throws IOException {
        if (!(address instanceof InetSocketAddress inetSocketAddress)) {
            throw new IllegalArgumentException("NetherNet requires an InetSocketAddress");
        }
        final InetAddress resolved = inetSocketAddress.getAddress();
        if (resolved == null) throw new IOException("Unresolved address: " + inetSocketAddress);

        final java.net.ProtocolFamily family = resolved.getAddress().length == 4
                ? StandardProtocolFamily.INET : StandardProtocolFamily.INET6;
        final DatagramChannel channel = DatagramChannel.open(family);
        try {
            channel.bind(inetSocketAddress);
        } catch (IOException e) {
            channel.close();
            throw e;
        }
        this.channel = channel;
        this.socketAddress = inetSocketAddress;
        this.address = inetSocketAddress.getHostString();
        this.port = ((InetSocketAddress) channel.getLocalAddress()).getPort();
    }

    @Override
    public void start() {
        Check.stateCondition(channel == null, "Transport must be bound before it is started");
        Thread.ofVirtual().name("Ms-NetherNet-Server").start(this::listenLoop);
    }

    private void listenLoop() {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(64 * 1024);
        while (!stop) {
            buffer.clear();
            final SocketAddress remote;
            try {
                remote = channel.receive(buffer);
            } catch (ClosedChannelException ignored) {
                break;
            } catch (IOException e) {
                if (stop) break;
                MinecraftServer.getExceptionManager().handleException(e);
                continue;
            }
            if (!(remote instanceof InetSocketAddress remoteAddress)) continue;
            buffer.flip();
            final byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            try {
                handleDatagram(remoteAddress, data);
            } catch (Exception e) {
                MinecraftServer.getExceptionManager().handleException(e);
            }
        }
    }

    private void handleDatagram(@NotNull InetSocketAddress remote, byte @NotNull [] data) {
        final NetherNetDiscoveryCodec.Decoded decoded = NetherNetDiscoveryCodec.decode(data);
        switch (decoded.packet()) {
            case NetherNetDiscoveryPacket.Request ignored ->
                    send(remote, NetherNetDiscoveryCodec.response(serverId, currentServerData()));
            case NetherNetDiscoveryPacket.Response ignored -> {
                // Servers do not consume discovery responses.
            }
            case NetherNetDiscoveryPacket.Message message ->
                    handleSignaling(remote, decoded.senderId(), message.data());
        }
    }

    private void handleSignaling(@NotNull InetSocketAddress remote, long clientId, @NotNull String rawMessage) {
        final NetherNetSignalingMessage message = NetherNetSignalingMessage.parse(rawMessage);
        switch (message.type()) {
            case CONNECT_REQUEST -> {
                final String connectionId = message.connectionId();
                sdpOffers.put(connectionId, message.data());
                final NetherNetSession session = new NetherNetSession(this, clientId, connectionId, remote);
                sessions.put(connectionId, session);

                try {
                    final NetherNetPeer peer = peerFactory.accept(session);
                    session.setPeer(peer);
                    sendSignaling(remote, clientId,
                            NetherNetSignalingMessage.connectResponse(connectionId, peer.sdpAnswer()).encode());
                } catch (Exception e) {
                    LOGGER.warn("Unable to accept NetherNet connection {} from {}", connectionId, remote, e);
                    session.close();
                }
            }
            case CONNECT_RESPONSE -> {
                // The server always answers offers; a response here is unexpected.
                LOGGER.debug("Ignoring unexpected NetherNet CONNECTRESPONSE for {}", message.connectionId());
            }
            case CANDIDATE_ADD -> {
                final NetherNetSession session = sessions.get(message.connectionId());
                if (session != null) session.addRemoteCandidate(message.data());
            }
        }
    }

    /**
     * Sends a signaling message to a client over LAN discovery.
     */
    public void sendSignaling(@NotNull InetSocketAddress remote, long recipientId, @NotNull String rawMessage) {
        final byte[] packet = NetherNetDiscoveryCodec.encode(serverId,
                new NetherNetDiscoveryPacket.Message(recipientId, rawMessage));
        send(remote, packet);
    }

    private void send(@NotNull InetSocketAddress remote, byte @NotNull [] data) {
        final DatagramChannel channel = this.channel;
        if (channel == null) return;
        try {
            synchronized (this) {
                channel.send(ByteBuffer.wrap(data), remote);
            }
        } catch (IOException e) {
            MinecraftServer.getExceptionManager().handleException(e);
        }
    }

    @NotNull String sdpOffer(@NotNull String connectionId) {
        return sdpOffers.getOrDefault(connectionId, "");
    }

    void registerSession(@NotNull NetherNetSession session) {
        sessions.put(session.connectionId(), session);
    }

    void removeSession(@NotNull NetherNetSession session) {
        sessions.remove(session.connectionId(), session);
        sdpOffers.remove(session.connectionId());
    }

    /**
     * All active sessions.
     */
    public @NotNull Collection<NetherNetSession> sessions() {
        return sessions.values();
    }

    /**
     * The packet handler used by new sessions.
     */
    public @Nullable BedrockPacketHandler packetHandler() {
        return packetHandler;
    }

    @Override
    public boolean isOpen() {
        return !stop;
    }

    @Override
    public void stop() {
        this.stop = true;
        for (NetherNetSession session : sessions.values()) {
            session.close();
        }
        sessions.clear();
        sdpOffers.clear();
        try {
            if (channel != null) channel.close();
        } catch (IOException e) {
            MinecraftServer.getExceptionManager().handleException(e);
        }
    }

    @Override
    public @Nullable SocketAddress socketAddress() {
        return socketAddress;
    }

    @Override
    public @Nullable String getAddress() {
        return address;
    }

    @Override
    public int getPort() {
        return port;
    }

    private NetherNetDiscoveryPacket.@NotNull ServerData currentServerData() {
        final NetherNetDiscoveryPacket.ServerData custom = this.serverData;
        return custom != null ? custom : defaultServerData();
    }

    private static NetherNetDiscoveryPacket.@NotNull ServerData defaultServerData() {
        return new NetherNetDiscoveryPacket.ServerData(
                1,
                MinecraftServer.getBrandName(),
                "world",
                0,
                MinecraftServer.getConnectionManager().getOnlinePlayerCount(),
                20,
                false,
                TRANSPORT_LAYER
        );
    }
}
