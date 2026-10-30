package net.minestom.server.protocol.bedrock.packet;

import org.jetbrains.annotations.NotNull;

/**
 * An opaque Bedrock packet, useful for packet implementations which only need the raw body.
 *
 * @param packetId the Bedrock packet id
 * @param payload  the packet body
 */
public record RawBedrockPacket(int packetId, byte @NotNull [] payload) implements BedrockServerPacket {
    public RawBedrockPacket {
        payload = payload.clone();
    }

    @Override
    public void write(@NotNull BedrockPacketWriter writer) {
        writer.writeBytes(payload);
    }
}
