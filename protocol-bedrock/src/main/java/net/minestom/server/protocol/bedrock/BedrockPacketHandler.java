package net.minestom.server.protocol.bedrock;

import net.minestom.server.protocol.bedrock.packet.BedrockPacketReader;
import org.jetbrains.annotations.NotNull;

/**
 * Receives decoded Bedrock packets for a connection.
 */
@FunctionalInterface
public interface BedrockPacketHandler {
    /**
     * Handles one packet.
     *
     * @param connection the connection which received the packet
     * @param packetId   the Bedrock packet id
     * @param reader     a reader positioned at the packet body
     */
    void handle(@NotNull BedrockPlayerConnection connection, int packetId, @NotNull BedrockPacketReader reader);
}
