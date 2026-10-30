package net.minestom.server.network.protocol;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.SocketAddress;

/**
 * A listener that accepts clients for one protocol family.
 * <p>
 * Implementations live in the protocol modules ({@code protocol-java},
 * {@code protocol-bedrock}) and are discovered through {@link TransportProvider}.
 * The core server only interacts with the transport through this interface, which keeps
 * game logic independent from the wire protocol.
 *
 * <p>Lifecycle: {@link #bind(SocketAddress)} once, then {@link #start()}, then
 * {@link #stop()}. A stopped transport must not be restarted.</p>
 */
public interface ServerTransport extends AutoCloseable {

    /**
     * The protocol family served by this transport.
     */
    @NotNull ProtocolFamily family();

    /**
     * The protocol version advertised by this transport.
     */
    @NotNull ProtocolVersion protocolVersion();

    /**
     * Binds the transport to the given address.
     *
     * @throws IOException if the underlying socket cannot be bound
     */
    void bind(@NotNull SocketAddress address) throws IOException;

    /**
     * Starts accepting clients on a virtual thread.
     */
    void start();

    /**
     * Stops accepting new clients and closes the listener.
     */
    void stop();

    /**
     * Returns whether this transport is currently accepting clients.
     */
    boolean isOpen();

    /**
     * The address the transport is bound to, or {@code null} before {@link #bind(SocketAddress)}.
     */
    @Nullable SocketAddress socketAddress();

    /**
     * A human-readable address, suitable for logs and query responses.
     */
    @Nullable String getAddress();

    /**
     * The bound port, or {@code 0} for transports which do not use a port.
     */
    int getPort();

    @Override
    default void close() {
        stop();
    }
}
