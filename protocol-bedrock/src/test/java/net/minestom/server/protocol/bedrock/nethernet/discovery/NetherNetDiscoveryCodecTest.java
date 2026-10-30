package net.minestom.server.protocol.bedrock.nethernet.discovery;

import net.minestom.server.protocol.bedrock.nethernet.NetherNetTransport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NetherNetDiscoveryCodecTest {

    @Test
    public void requestRoundTrip() {
        byte[] encoded = NetherNetDiscoveryCodec.encode(0x1234567890ABCDEFL,
                new NetherNetDiscoveryPacket.Request());
        NetherNetDiscoveryCodec.Decoded decoded = NetherNetDiscoveryCodec.decode(encoded);
        assertEquals(0x1234567890ABCDEFL, decoded.senderId());
        assertInstanceOf(NetherNetDiscoveryPacket.Request.class, decoded.packet());
    }

    @Test
    public void responseRoundTrip() {
        NetherNetDiscoveryPacket.ServerData data = new NetherNetDiscoveryPacket.ServerData(
                1, "Minestom", "world", 1, 3, 20, false, NetherNetTransport.TRANSPORT_LAYER);
        byte[] encoded = NetherNetDiscoveryCodec.response(42L,
                data);
        NetherNetDiscoveryCodec.Decoded decoded = NetherNetDiscoveryCodec.decode(encoded);
        assertEquals(42L, decoded.senderId());
        assertInstanceOf(NetherNetDiscoveryPacket.Response.class, decoded.packet());
        assertEquals(data, ((NetherNetDiscoveryPacket.Response) decoded.packet()).serverData());
    }

    @Test
    public void messageRoundTrip() {
        byte[] encoded = NetherNetDiscoveryCodec.encode(7L,
                new NetherNetDiscoveryPacket.Message(9L, "CONNECTREQUEST 1 sdp-offer"));
        NetherNetDiscoveryCodec.Decoded decoded = NetherNetDiscoveryCodec.decode(encoded);
        assertEquals(7L, decoded.senderId());
        assertEquals(new NetherNetDiscoveryPacket.Message(9L, "CONNECTREQUEST 1 sdp-offer"), decoded.packet());
    }

    @Test
    public void rejectsTamperedPacket() {
        byte[] encoded = NetherNetDiscoveryCodec.encode(1L, new NetherNetDiscoveryPacket.Request());
        encoded[encoded.length - 1] ^= 0x7F;
        assertThrows(IllegalArgumentException.class, () -> NetherNetDiscoveryCodec.decode(encoded));
    }
}
