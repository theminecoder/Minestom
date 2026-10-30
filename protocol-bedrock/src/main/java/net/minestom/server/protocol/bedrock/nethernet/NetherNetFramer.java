package net.minestom.server.protocol.bedrock.nethernet;

import net.minestom.server.protocol.bedrock.packet.BedrockPacketReader;
import net.minestom.server.protocol.bedrock.packet.BedrockPacketWriter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Frames Bedrock packets for NetherNet's SCTP data channels.
 * <p>
 * Unlike RakNet, NetherNet never batches packets: each SCTP message carries one packet.
 * Packets larger than {@link #MAX_MESSAGE_SIZE} are split into segments. Every message has
 * the following layout:
 * <pre>
 *   segment count (uint8)
 *   packet length (varuint32)
 *   packet data
 * </pre>
 * The segment count is the number of segments still to come (zero means this message
 * completes the packet).
 */
public final class NetherNetFramer {
    /**
     * The maximum payload of a single SCTP message before segmentation.
     */
    public static final int MAX_MESSAGE_SIZE = 10_000;

    private NetherNetFramer() {
    }

    /**
     * Splits a packet into one or more SCTP messages.
     */
    public static @NotNull List<byte[]> frame(byte @NotNull [] packet) {
        if (packet.length <= MAX_MESSAGE_SIZE) {
            return List.of(encodeSegment(0, packet, 0, packet.length));
        }
        final List<byte[]> messages = new ArrayList<>();
        int offset = 0;
        while (offset < packet.length) {
            final int length = Math.min(MAX_MESSAGE_SIZE, packet.length - offset);
            final int remaining = (packet.length - (offset + length) + MAX_MESSAGE_SIZE - 1) / MAX_MESSAGE_SIZE;
            messages.add(encodeSegment(remaining, packet, offset, length));
            offset += length;
        }
        return messages;
    }

    private static byte @NotNull [] encodeSegment(int remainingSegments, byte[] packet, int offset, int length) {
        final BedrockPacketWriter writer = new BedrockPacketWriter(length + 5);
        writer.writeByte(remainingSegments);
        writer.writeVarInt(length);
        writer.writeBytes(packet, offset, length);
        return writer.toByteArray();
    }

    /**
     * Reads one SCTP message.
     */
    public static @NotNull ParsedMessage parse(byte @NotNull [] message) {
        final BedrockPacketReader reader = new BedrockPacketReader(message);
        final int remainingSegments = reader.readByte();
        final int length = Math.toIntExact(reader.readVarInt());
        if (length < 0 || length > reader.remainingBytes()) {
            throw new IllegalArgumentException("Invalid NetherNet segment length " + length
                    + " (remaining " + reader.remainingBytes() + ")");
        }
        return new ParsedMessage(remainingSegments, reader.readBytes(length));
    }

    /**
     * A decoded SCTP message.
     *
     * @param remainingSegments number of segments which still follow this message
     * @param payload           this message's slice of the packet
     */
    public record ParsedMessage(int remainingSegments, byte @NotNull [] payload) {
    }
}
