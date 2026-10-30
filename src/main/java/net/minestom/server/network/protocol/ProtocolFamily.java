package net.minestom.server.network.protocol;

import org.jetbrains.annotations.NotNull;

/**
 * The edition of Minecraft a connection speaks.
 * <p>
 * A single Minestom process may host transports for more than one family at the same time,
 * for example a Java Edition TCP listener on {@code 25565} and a Bedrock Edition
 * NetherNet listener on {@code 19132}.
 */
public enum ProtocolFamily {
    JAVA("java"),
    BEDROCK("bedrock");

    private final String id;

    ProtocolFamily(@NotNull String id) {
        this.id = id;
    }

    /**
     * A stable, lower-case identifier suitable for configuration files and logs.
     */
    public @NotNull String id() {
        return id;
    }
}
