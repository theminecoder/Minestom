package net.minestom.server.network.protocol;

import org.jetbrains.annotations.NotNull;

/**
 * Service-loader entry point for a protocol module.
 * <p>
 * Each protocol module registers exactly one provider in
 * {@code META-INF/services/net.minestom.server.network.protocol.TransportProvider}.
 * The core server loads providers lazily the first time a transport is requested.
 */
public interface TransportProvider {

    /**
     * The protocol family handled by this provider.
     */
    @NotNull ProtocolFamily family();

    /**
     * The protocol version implemented by this provider.
     */
    @NotNull ProtocolVersion protocolVersion();

    /**
     * Creates a new, unbound transport.
     */
    @NotNull ServerTransport create();
}
