package net.minestom.server.protocol.bedrock.packet;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

/**
 * Little-endian writer for the Bedrock Edition wire format.
 * <p>
 * Bedrock uses unsigned LEB128 varints (not Java Edition's fixed-width format) for most
 * lengths and ids, and little-endian fixed-width integers everywhere else.
 */
public final class BedrockPacketWriter {
    private byte[] buffer;
    private int size;

    public BedrockPacketWriter() {
        this(64);
    }

    public BedrockPacketWriter(int initialCapacity) {
        this.buffer = new byte[Math.max(8, initialCapacity)];
    }

    public @NotNull BedrockPacketWriter writeByte(int value) {
        ensure(1);
        buffer[size++] = (byte) value;
        return this;
    }

    public @NotNull BedrockPacketWriter writeBoolean(boolean value) {
        return writeByte(value ? 1 : 0);
    }

    public @NotNull BedrockPacketWriter writeShort(int value) {
        ensure(2);
        buffer[size++] = (byte) value;
        buffer[size++] = (byte) (value >>> 8);
        return this;
    }

    public @NotNull BedrockPacketWriter writeInt(int value) {
        ensure(4);
        buffer[size++] = (byte) value;
        buffer[size++] = (byte) (value >>> 8);
        buffer[size++] = (byte) (value >>> 16);
        buffer[size++] = (byte) (value >>> 24);
        return this;
    }

    public @NotNull BedrockPacketWriter writeLong(long value) {
        ensure(8);
        for (int i = 0; i < 8; i++) {
            buffer[size++] = (byte) (value >>> (i * 8));
        }
        return this;
    }

    public @NotNull BedrockPacketWriter writeFloat(float value) {
        return writeInt(Float.floatToRawIntBits(value));
    }

    public @NotNull BedrockPacketWriter writeDouble(double value) {
        return writeLong(Double.doubleToRawLongBits(value));
    }

    /**
     * Writes an unsigned LEB128 varint.
     */
    public @NotNull BedrockPacketWriter writeVarInt(long value) {
        if (value < 0) throw new IllegalArgumentException("varint must be unsigned: " + value);
        while (value >= 0x80) {
            writeByte((int) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        writeByte((int) value);
        return this;
    }

    /**
     * Writes a signed value using zig-zag encoding followed by an unsigned varint.
     */
    public @NotNull BedrockPacketWriter writeSignedVarInt(int value) {
        return writeVarInt(((long) value << 1) ^ (value >> 31));
    }

    public @NotNull BedrockPacketWriter writeString(@NotNull String value) {
        final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(bytes.length);
        writeBytes(bytes);
        return this;
    }

    public @NotNull BedrockPacketWriter writeBytes(byte @NotNull [] bytes) {
        ensure(bytes.length);
        System.arraycopy(bytes, 0, buffer, size, bytes.length);
        size += bytes.length;
        return this;
    }

    public @NotNull BedrockPacketWriter writeBytes(byte @NotNull [] bytes, int offset, int length) {
        ensure(length);
        System.arraycopy(bytes, offset, buffer, size, length);
        size += length;
        return this;
    }

    public int size() {
        return size;
    }

    public byte @NotNull [] toByteArray() {
        final byte[] result = new byte[size];
        System.arraycopy(buffer, 0, result, 0, size);
        return result;
    }

    private void ensure(int additional) {
        final int required = size + additional;
        if (required <= buffer.length) return;
        int newLength = buffer.length;
        while (newLength < required) newLength <<= 1;
        final byte[] newBuffer = new byte[newLength];
        System.arraycopy(buffer, 0, newBuffer, 0, size);
        buffer = newBuffer;
    }
}
