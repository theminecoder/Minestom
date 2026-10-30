package net.minestom.server.protocol.bedrock.nethernet;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NetherNetFramerTest {

    @Test
    public void smallPacketIsSingleSegment() {
        byte[] packet = new byte[]{1, 2, 3, 4};
        List<byte[]> messages = NetherNetFramer.frame(packet);
        assertEquals(1, messages.size());

        NetherNetFramer.ParsedMessage parsed = NetherNetFramer.parse(messages.getFirst());
        assertEquals(0, parsed.remainingSegments());
        assertArrayEquals(packet, parsed.payload());
    }

    @Test
    public void largePacketIsSegmentedAndReassembles() {
        byte[] packet = new byte[NetherNetFramer.MAX_MESSAGE_SIZE * 2 + 17];
        for (int i = 0; i < packet.length; i++) packet[i] = (byte) i;

        List<byte[]> messages = NetherNetFramer.frame(packet);
        assertEquals(3, messages.size());

        byte[] reassembled = new byte[packet.length];
        int offset = 0;
        for (int i = 0; i < messages.size(); i++) {
            NetherNetFramer.ParsedMessage parsed = NetherNetFramer.parse(messages.get(i));
            assertEquals(messages.size() - i - 1, parsed.remainingSegments());
            System.arraycopy(parsed.payload(), 0, reassembled, offset, parsed.payload().length);
            offset += parsed.payload().length;
        }
        assertArrayEquals(packet, reassembled);
    }

    @Test
    public void rejectsTruncatedSegment() {
        assertThrows(IllegalArgumentException.class,
                () -> NetherNetFramer.parse(new byte[]{0, 5, 1, 2}));
    }
}
