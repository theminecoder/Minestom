package net.minestom.server.extras.query.response;

import net.minestom.server.MinecraftServer;
import net.minestom.server.network.protocol.ServerTransport;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * An enum of default query keys.
 */
public enum QueryKey {
    HOSTNAME(() -> "A Minestom Server"),
    GAME_TYPE(() -> "SMP"),
    GAME_ID("game_id", () -> "MINECRAFT"),
    VERSION(() -> MinecraftServer.VERSION_NAME),
    PLUGINS(FullQueryResponse::generatePluginsValue),
    MAP(() -> "world"),
    NUM_PLAYERS("numplayers", () -> String.valueOf(MinecraftServer.getConnectionManager().getOnlinePlayerCount())),
    MAX_PLAYERS("maxplayers", () -> String.valueOf(MinecraftServer.getConnectionManager().getOnlinePlayerCount() + 1)),
    HOST_PORT("hostport", QueryKey::serverPort),
    HOST_IP("hostip", QueryKey::serverIp);

    static QueryKey[] VALUES = QueryKey.values();

    private final String key;
    private final Supplier<String> value;

    QueryKey(Supplier<String> value) {
        this(null, value);
    }

    QueryKey(@Nullable String key, Supplier<String> value) {
        this.key = Objects.requireNonNullElse(key, this.name().toLowerCase(Locale.ROOT).replace('_', ' '));
        this.value = value;
    }

    private static String serverPort() {
        final ServerTransport server = MinecraftServer.getServer();
        return String.valueOf(server != null ? server.getPort() : 0);
    }

    private static String serverIp() {
        final ServerTransport server = MinecraftServer.getServer();
        return Objects.requireNonNullElse(server != null ? server.getAddress() : null, "localhost");
    }

    /**
     * Gets the key of this query key.
     *
     * @return the key
     */
    public String getKey() {
        return this.key;
    }

    /**
     * Gets the value of this query key.
     *
     * @return the value
     */
    public String getValue() {
        return this.value.get();
    }
}
