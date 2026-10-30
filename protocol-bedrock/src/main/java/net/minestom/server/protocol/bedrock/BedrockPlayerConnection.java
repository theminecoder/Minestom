package net.minestom.server.protocol.bedrock;

import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.network.protocol.ProtocolFamily;
import net.minestom.server.network.protocol.ProtocolVersion;
import net.minestom.server.protocol.bedrock.nethernet.NetherNetSession;
import net.minestom.server.protocol.bedrock.packet.BedrockPacketReader;
import net.minestom.server.protocol.bedrock.packet.BedrockPacketWriter;
import net.minestom.server.protocol.bedrock.packet.BedrockServerPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.Collection;

/**
 * A player connection backed by a NetherNet session.
 * <p>
 * Bedrock packets are a different wire format from Java Edition packets. This connection
 * therefore accepts {@link BedrockServerPacket}s and rejects Java {@code ServerPacket}s;
 * game code which wants to be protocol-independent should use the shared APIs on
 * {@link net.minestom.server.entity.Player} instead of sending raw packets.
 */
public final class BedrockPlayerConnection extends PlayerConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(BedrockPlayerConnection.class);

    private final NetherNetSession session;
    private volatile BedrockVersion version = BedrockVersion.CURRENT;
    private volatile BedrockPacketHandler packetHandler;

    public BedrockPlayerConnection(@NotNull NetherNetSession session) {
        this.session = session;
    }

    /**
     * The NetherNet session which carries this connection.
     */
    public @NotNull NetherNetSession session() {
        return session;
    }

    /**
     * Sets the handler for incoming packets.
     */
    public void setPacketHandler(@Nullable BedrockPacketHandler packetHandler) {
        this.packetHandler = packetHandler;
    }

    /**
     * Updates the client's protocol version, usually after the login packet is read.
     */
    public void setVersion(@NotNull BedrockVersion version) {
        this.version = version;
    }

    /**
     * Called by the NetherNet session with one complete Bedrock packet.
     */
    public void receivePacket(byte @NotNull [] packet) {
        final BedrockPacketReader reader = new BedrockPacketReader(packet);
        final int packetId = Math.toIntExact(reader.readVarInt());
        final BedrockPacketHandler handler = this.packetHandler;
        if (handler == null) {
            LOGGER.debug("Ignoring Bedrock packet 0x{}: no packet handler installed", Integer.toHexString(packetId));
            return;
        }
        handler.handle(this, packetId, reader);
    }

    @Override
    public void sendPacket(@NotNull SendablePacket packet) {
        final byte[] encoded;
        switch (packet) {
            case BedrockServerPacket bedrockPacket -> {
                final BedrockPacketWriter writer = new BedrockPacketWriter();
                writer.writeVarInt(bedrockPacket.packetId());
                bedrockPacket.write(writer);
                encoded = writer.toByteArray();
            }
            default -> throw new UnsupportedOperationException(
                    "Cannot send Java Edition packet " + packet.getClass().getName()
                            + " to a Bedrock client. Use a BedrockServerPacket.");
        }
        session.send(encoded);
    }

    @Override
    public void sendPackets(@NotNull Collection<SendablePacket> packets) {
        for (SendablePacket packet : packets) sendPacket(packet);
    }

    @Override
    public @NotNull ProtocolFamily getProtocolFamily() {
        return ProtocolFamily.BEDROCK;
    }

    @Override
    public @NotNull ProtocolVersion getProtocol() {
        return version.toProtocolVersion();
    }

    @Override
    public int getProtocolVersion() {
        return version.protocol();
    }

    @Override
    public @NotNull SocketAddress getRemoteAddress() {
        return session.remoteAddress();
    }

    @Override
    public @Nullable String getServerAddress() {
        return session.transport().getAddress();
    }

    @Override
    public int getServerPort() {
        return session.transport().getPort();
    }

    @Override
    public void kick(@NotNull Component component) {
        // Bedrock has no shared disconnect packet representation yet. Close the session;
        // a future Bedrock DisconnectPacket implementation can be sent here.
        LOGGER.debug("Kicking Bedrock player: {}", component);
        disconnect();
    }

    @Override
    public void disconnect() {
        super.disconnect();
        session.close();
    }
}
