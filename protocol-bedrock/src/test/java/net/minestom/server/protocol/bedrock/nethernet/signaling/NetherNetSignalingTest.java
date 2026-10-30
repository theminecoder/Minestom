package net.minestom.server.protocol.bedrock.nethernet.signaling;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NetherNetSignalingTest {

    @Test
    public void parsesConnectRequest() {
        NetherNetSignalingMessage message = NetherNetSignalingMessage.parse(
                "CONNECTREQUEST abc123 v=0\r\no=- 1 2 IN IP4 127.0.0.1\r\n");
        assertEquals(NetherNetSignalingMessage.Type.CONNECT_REQUEST, message.type());
        assertEquals("abc123", message.connectionId());
        assertTrue(message.data().startsWith("v=0"));
    }

    @Test
    public void parsesCandidate() {
        NetherNetSignalingMessage message = NetherNetSignalingMessage.parse(
                "CANDIDATEADD c1 candidate:1 1 udp 1 127.0.0.1 1234 typ host");
        assertEquals(NetherNetSignalingMessage.Type.CANDIDATE_ADD, message.type());
        assertEquals("c1", message.connectionId());
    }

    @Test
    public void roundTrips() {
        NetherNetSignalingMessage original = NetherNetSignalingMessage.connectResponse("id", "answer");
        assertEquals(original, NetherNetSignalingMessage.parse(original.encode()));
    }

    @Test
    public void rejectsMalformed() {
        assertThrows(IllegalArgumentException.class, () -> NetherNetSignalingMessage.parse("CONNECTREQUEST"));
        assertThrows(IllegalArgumentException.class, () -> NetherNetSignalingMessage.parse("NOPE id data"));
    }
}
