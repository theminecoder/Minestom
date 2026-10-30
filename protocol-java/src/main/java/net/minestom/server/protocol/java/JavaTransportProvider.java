package net.minestom.server.protocol.java;

import net.minestom.server.network.protocol.ProtocolFamily;
import net.minestom.server.network.protocol.ProtocolVersion;
import net.minestom.server.network.protocol.ServerTransport;
import net.minestom.server.network.protocol.TransportProvider;
import org.jetbrains.annotations.NotNull;

/**
 * Service-loader provider for the Java Edition TCP transport.
 */
public final class JavaTransportProvider implements TransportProvider {
    @Override
    public @NotNull ProtocolFamily family() {
        return ProtocolFamily.JAVA;
    }

    @Override
    public @NotNull ProtocolVersion protocolVersion() {
        return JavaProtocol.VERSION;
    }

    @Override
    public @NotNull ServerTransport create() {
        return new JavaServerTransport();
    }
}
