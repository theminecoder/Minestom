package net.minestom.server.protocol.bedrock.packet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BedrockPacketCodecTest {

    @Test
    public void varIntRoundTrip() {
        long[] values = {0, 1, 127, 128, 255, 300, 16_383, 16_384, 1L << 40};
        for (long value : values) {
            BedrockPacketWriter writer = new BedrockPacketWriter();
            writer.writeVarInt(value);
            BedrockPacketReader reader = new BedrockPacketReader(writer.toByteArray());
            assertEquals(value, reader.readVarInt());
            assertEquals(0, reader.remainingBytes());
        }
    }

    @Test
    public void signedVarIntRoundTrip() {
        int[] values = {0, -1, 1, Integer.MIN_VALUE, Integer.MAX_VALUE};
        for (int value : values) {
            BedrockPacketWriter writer = new BedrockPacketWriter();
            writer.writeSignedVarInt(value);
            BedrockPacketReader reader = new BedrockPacketReader(writer.toByteArray());
            assertEquals(value, reader.readSignedVarInt());
        }
    }

    @Test
    public void primitivesRoundTrip() {
        BedrockPacketWriter writer = new BedrockPacketWriter();
        writer.writeByte(0xAB)
                .writeShort(0xBEEF)
                .writeInt(0xDEADBEEF)
                .writeLong(0x0123456789ABCDEFL)
                .writeFloat(1.5F)
                .writeDouble(2.5D)
                .writeString("hello \u00e9")
                .writeBoolean(true);

        BedrockPacketReader reader = new BedrockPacketReader(writer.toByteArray());
        assertEquals(0xAB, reader.readByte());
        assertEquals(0xBEEF, reader.readShort());
        assertEquals(0xDEADBEEF, reader.readInt());
        assertEquals(0x0123456789ABCDEFL, reader.readLong());
        assertEquals(1.5F, reader.readFloat());
        assertEquals(2.5D, reader.readDouble());
        assertEquals("hello \u00e9", reader.readString());
        assertTrue(reader.readBoolean());
        assertEquals(0, reader.remainingBytes());
    }

    @Test
    public void rawPacketIsCopySafe() {
        byte[] payload = {1, 2, 3};
        RawBedrockPacket packet = new RawBedrockPacket(0x01, payload);
        payload[0] = 9;
        assertEquals(1, packet.payload()[0]);
    }
}
