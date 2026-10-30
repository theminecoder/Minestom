package net.minestom.server.protocol.bedrock.nethernet;

import net.minestom.server.network.protocol.ProtocolFamily;
import net.minestom.server.network.protocol.ProtocolVersion;
import net.minestom.server.network.protocol.ServerTransport;
import net.minestom.server.network.protocol.TransportProvider;
import net.minestom.server.protocol.bedrock.BedrockProtocol;
import org.jetbrains.annotations.NotNull;

/**
 * Service-loader provider for the Bedrock Edition NetherNet transport.
 */
public final class NetherNetTransportProvider implements TransportProvider {
    @Override
    public @NotNull ProtocolFamily family() {
        return ProtocolFamily.BEDROCK;
    }

    @Override
    public @NotNull ProtocolVersion protocolVersion() {
        return BedrockProtocol.VERSION;
    }

    @Override
    public @NotNull ServerTransport create() {
        return new NetherNetTransport();
    }
}
