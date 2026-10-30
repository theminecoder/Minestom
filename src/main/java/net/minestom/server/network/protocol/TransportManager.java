package net.minestom.server.network.protocol;

import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry and lifecycle owner for {@link ServerTransport}s.
 * <p>
 * Providers are discovered with {@link ServiceLoader}, so adding a protocol module to the
 * classpath is enough to make its transport available. Custom transports may also be
 * registered explicitly before the server starts.
 */
public final class TransportManager {
    private final Map<ProtocolFamily, TransportProvider> providers = new ConcurrentHashMap<>();
    private final List<ServerTransport> transports = new CopyOnWriteArrayList<>();
    private volatile boolean providersDiscovered;

    /**
     * Creates a transport manager with the built-in transports discovered from the classpath.
     */
    public TransportManager() {
        discoverProviders();
    }

    /**
     * Discovers all transport providers on the classpath.
     * <p>Calling this method more than once is safe; providers are only loaded once.</p>
     */
    public synchronized void discoverProviders() {
        if (providersDiscovered) return;
        providersDiscovered = true;

        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final ServiceLoader<TransportProvider> serviceLoader = ServiceLoader.load(
                TransportProvider.class,
                loader != null ? loader : TransportProvider.class.getClassLoader()
        );
        for (TransportProvider provider : serviceLoader) {
            registerProvider(provider);
        }
    }

    /**
     * Registers a provider explicitly. A later provider for the same family replaces the
     * previous one, which makes it possible for applications to override a built-in transport.
     */
    public void registerProvider(@NotNull TransportProvider provider) {
        providers.put(provider.family(), provider);
    }

    /**
     * Returns whether a provider is available for the given family.
     */
    public boolean hasProvider(@NotNull ProtocolFamily family) {
        return providers.containsKey(family);
    }

    /**
     * The protocol families for which a provider is available.
     */
    public @NotNull Set<ProtocolFamily> families() {
        return Collections.unmodifiableSet(providers.keySet());
    }

    /**
     * Creates a new transport for the given family.
     *
     * @throws IllegalStateException if no provider is registered for the family
     */
    public @NotNull ServerTransport create(@NotNull ProtocolFamily family) {
        final TransportProvider provider = providers.get(family);
        if (provider == null) {
            throw new IllegalStateException("No transport provider registered for protocol family '"
                    + family.id() + "'. Add a dependency on the corresponding protocol module "
                    + "('protocol-" + family.id() + "') or register a TransportProvider manually.");
        }
        return provider.create();
    }

    /**
     * Creates, binds and starts a transport for the given family.
     *
     * @throws IOException           if the address cannot be bound
     * @throws IllegalStateException if the family has no provider
     */
    public @NotNull ServerTransport start(@NotNull ProtocolFamily family, @NotNull SocketAddress address) throws IOException {
        final ServerTransport transport = create(family);
        transport.bind(address);
        addTransport(transport);
        transport.start();
        MinecraftServer.LOGGER.info("Started {} transport on {}", family.id(), describe(transport));
        return transport;
    }

    /**
     * Registers an externally created transport, for example one that has already been bound.
     */
    public void addTransport(@NotNull ServerTransport transport) {
        transports.add(transport);
    }

    /**
     * All transports created by this manager.
     */
    public @NotNull Collection<ServerTransport> transports() {
        return Collections.unmodifiableList(transports);
    }

    /**
     * Finds the first transport for the given family, if any.
     */
    public @NotNull Optional<ServerTransport> transport(@NotNull ProtocolFamily family) {
        return transports.stream().filter(transport -> transport.family() == family).findFirst();
    }

    /**
     * The transport used for APIs which expose a single server address.
     * <p>Java is preferred to preserve the historical behaviour of
     * {@link MinecraftServer#getServer()}; otherwise the first registered transport is used.</p>
     */
    public @Nullable ServerTransport primary() {
        for (ServerTransport transport : transports) {
            if (transport.family() == ProtocolFamily.JAVA) return transport;
        }
        return transports.isEmpty() ? null : transports.getFirst();
    }

    /**
     * Stops every managed transport.
     */
    public void stopAll() {
        for (ServerTransport transport : transports) {
            try {
                transport.stop();
            } catch (Exception e) {
                MinecraftServer.getExceptionManager().handleException(e);
            }
        }
        transports.clear();
    }

    private static @NotNull String describe(@NotNull ServerTransport transport) {
        final String address = transport.getAddress();
        return address != null ? address : String.valueOf(transport.socketAddress());
    }
}
