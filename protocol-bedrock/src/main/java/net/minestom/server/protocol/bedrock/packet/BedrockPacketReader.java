package net.minestom.server.protocol.bedrock.packet;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Little-endian reader for the Bedrock Edition wire format.
 */
public final class BedrockPacketReader {
    private final byte[] buffer;
    private int index;
    private final int end;

    public BedrockPacketReader(byte @NotNull [] buffer) {
        this(buffer, 0, buffer.length);
    }

    public BedrockPacketReader(byte @NotNull [] buffer, int offset, int length) {
        if (offset < 0 || length < 0 || offset + length > buffer.length) {
            throw new IndexOutOfBoundsException("Invalid slice: offset=" + offset + ", length=" + length);
        }
        this.buffer = buffer;
        this.index = offset;
        this.end = offset + length;
    }

    public int readByte() {
        require(1);
        return buffer[index++] & 0xFF;
    }

    public boolean readBoolean() {
        return readByte() != 0;
    }

    public int readShort() {
        require(2);
        final int value = (buffer[index] & 0xFF) | ((buffer[index + 1] & 0xFF) << 8);
        index += 2;
        return value;
    }

    public int readInt() {
        require(4);
        final int value = (buffer[index] & 0xFF)
                | ((buffer[index + 1] & 0xFF) << 8)
                | ((buffer[index + 2] & 0xFF) << 16)
                | ((buffer[index + 3] & 0xFF) << 24);
        index += 4;
        return value;
    }

    public long readLong() {
        require(8);
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value |= (long) (buffer[index + i] & 0xFF) << (i * 8);
        }
        index += 8;
        return value;
    }

    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    /**
     * Reads an unsigned LEB128 varint.
     */
    public long readVarInt() {
        long value = 0;
        int shift = 0;
        while (true) {
            final int b = readByte();
            value |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return value;
            shift += 7;
            if (shift > 63) throw new IllegalArgumentException("VarInt is too big");
        }
    }

    /**
     * Reads a signed zig-zag varint.
     */
    public int readSignedVarInt() {
        final long value = readVarInt();
        return (int) ((value >>> 1) ^ -(value & 1));
    }
    public @NotNull String readString() {
        final int length = Math.toIntExact(readVarInt());
        require(length);
        final String value = new String(buffer, index, length, StandardCharsets.UTF_8);
        index += length;
        return value;
    }

    public byte @NotNull [] readBytes(int length) {
        require(length);
        final byte[] result = Arrays.copyOfRange(buffer, index, index + length);
        index += length;
        return result;
    }

    public byte @NotNull [] remaining() {
        final byte[] result = Arrays.copyOfRange(buffer, index, end);
        index = end;
        return result;
    }

    public int remainingBytes() {
        return end - index;
    }

    private void require(int length) {
        if (index + length > end) {
            throw new IndexOutOfBoundsException("Not enough bytes: need " + length + ", have " + (end - index));
        }
    }
}
