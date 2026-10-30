package net.minestom.server.protocol.bedrock.packet;

import net.minestom.server.network.packet.server.SendablePacket;
import org.jetbrains.annotations.NotNull;

/**
 * A packet which can be sent to a Bedrock Edition client.
 * <p>
 * Bedrock packets are not {@link net.minestom.server.network.packet.server.ServerPacket}s
 * (those describe the Java Edition wire format). Implementing this interface is enough for
 * {@link net.minestom.server.protocol.bedrock.BedrockPlayerConnection} to encode and send
 * the packet over NetherNet.
 */
public interface BedrockServerPacket extends SendablePacket {
    /**
     * The Bedrock packet id.
     */
    int packetId();

    /**
     * Writes the packet body (without id or length) to the writer.
     */
    void write(@NotNull BedrockPacketWriter writer);
}
