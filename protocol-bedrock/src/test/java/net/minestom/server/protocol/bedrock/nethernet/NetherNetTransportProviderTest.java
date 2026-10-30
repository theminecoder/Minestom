package net.minestom.server.protocol.bedrock.nethernet;

import net.minestom.server.network.protocol.ProtocolFamily;
import net.minestom.server.network.protocol.TransportManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NetherNetTransportProviderTest {

    @Test
    public void serviceLoaderDiscoversProvider() {
        TransportManager transports = new TransportManager();
        assertTrue(transports.hasProvider(ProtocolFamily.BEDROCK));
        assertInstanceOf(NetherNetTransport.class, transports.create(ProtocolFamily.BEDROCK));
    }
}
