package net.minestom.server.protocol.bedrock.nethernet;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.protocol.bedrock.BedrockPlayerConnection;
import net.minestom.server.protocol.bedrock.nethernet.signaling.NetherNetSignalingMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One NetherNet client connection.
 * <p>
 * The session owns the WebRTC peer, reassembles SCTP segments, and creates the
 * {@link BedrockPlayerConnection} once data starts flowing. A session exists before the
 * player is authenticated (Bedrock authenticates inside the Bedrock login packet, not in a
 * separate connection state), so it is intentionally independent from {@link Player}.
 */
public final class NetherNetSession {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetherNetSession.class);

    private final NetherNetTransport transport;
    private final long networkId;
    private final String connectionId;
    private final InetSocketAddress remoteAddress;

    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile NetherNetPeer peer;
    private volatile BedrockPlayerConnection connection;

    // Reassembly state for the reliable channel.
    private final ByteArrayOutputStream pendingPacket = new ByteArrayOutputStream();
    private int pendingSegments;

    NetherNetSession(
            @NotNull NetherNetTransport transport,
            long networkId,
            @NotNull String connectionId,
            @NotNull InetSocketAddress remoteAddress
    ) {
        this.transport = transport;
        this.networkId = networkId;
        this.connectionId = connectionId;
        this.remoteAddress = remoteAddress;
    }

    public long networkId() {
        return networkId;
    }

    public @NotNull String connectionId() {
        return connectionId;
    }

    public @NotNull InetSocketAddress remoteAddress() {
        return remoteAddress;
    }

    public @NotNull NetherNetTransport transport() {
        return transport;
    }

    /**
     * The SDP offer sent by the client.
     */
    public @NotNull String sdpOffer() {
        return transport.sdpOffer(connectionId);
    }

    /**
     * Sends a signaling message back to this client over the LAN signaling channel.
     */
    public void sendSignaling(@NotNull NetherNetSignalingMessage message) {
        transport.sendSignaling(remoteAddress, networkId, message.encode());
    }

    /**
     * Sends raw SCTP data over the reliable channel.
     */
    public void send(byte @NotNull [] packet) {
        final NetherNetPeer peer = this.peer;
        if (peer == null) throw new IllegalStateException("NetherNet peer is not connected");
        try {
            for (byte[] message : NetherNetFramer.frame(packet)) {
                peer.send(message, true);
            }
        } catch (IOException e) {
            close(e);
        }
    }

    /**
     * Called by the peer factory once the WebRTC peer is ready.
     */
    public void setPeer(@NotNull NetherNetPeer peer) {
        this.peer = peer;
        this.transport.registerSession(this);
    }

    /**
     * Called by the peer factory when the client sends an SCTP message.
     */
    public void receiveData(byte @NotNull [] message) {
        if (closed.get()) return;
        final NetherNetFramer.ParsedMessage parsed = NetherNetFramer.parse(message);
        if (pendingSegments != 0 && parsed.remainingSegments() == 0) {
            // Last segment of a multi-message packet.
            pendingPacket.writeBytes(parsed.payload());
            final byte[] packet = pendingPacket.toByteArray();
            pendingPacket.reset();
            pendingSegments = 0;
            handlePacket(packet);
            return;
        }
        if (parsed.remainingSegments() > 0) {
            pendingPacket.writeBytes(parsed.payload());
            pendingSegments = parsed.remainingSegments();
            return;
        }
        handlePacket(parsed.payload());
    }

    private void handlePacket(byte @NotNull [] packet) {
        BedrockPlayerConnection connection = this.connection;
        if (connection == null) {
            synchronized (this) {
                connection = this.connection;
                if (connection == null) {
                    connection = new BedrockPlayerConnection(this);
                    connection.setPacketHandler(transport.packetHandler());
                    this.connection = connection;
                }
            }
        }
        try {
            connection.receivePacket(packet);
        } catch (Exception e) {
            MinecraftServer.getExceptionManager().handleException(e);
            close(e);
        }
    }

    /**
     * Called by the peer factory when a remote ICE candidate arrives.
     */
    public void addRemoteCandidate(@NotNull String candidate) {
        final NetherNetPeer peer = this.peer;
        if (peer == null) {
            LOGGER.debug("Ignoring ICE candidate for session {} before the peer is ready", connectionId);
            return;
        }
        try {
            peer.addRemoteCandidate(candidate);
        } catch (IOException e) {
            close(e);
        }
    }

    public @Nullable BedrockPlayerConnection connection() {
        return connection;
    }

    public boolean isClosed() {
        return closed.get();
    }

    /**
     * Closes the session and removes it from the transport.
     */
    public void close() {
        close(null);
    }

    private void close(@Nullable Throwable cause) {
        if (!closed.compareAndSet(false, true)) return;
        if (cause != null) LOGGER.debug("Closing NetherNet session {}", connectionId, cause);
        final NetherNetPeer peer = this.peer;
        if (peer != null) {
            try {
                peer.close();
            } catch (Exception e) {
                LOGGER.debug("Error closing NetherNet peer", e);
            }
        }
        final BedrockPlayerConnection connection = this.connection;
        if (connection != null) connection.disconnect();
        transport.removeSession(this);
    }

    @Override
    public String toString() {
        return "NetherNetSession{" + connectionId + ", " + remoteAddress + "}";
    }
}
