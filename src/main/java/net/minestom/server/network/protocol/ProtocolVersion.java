package net.minestom.server.network.protocol;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Set;

/**
 * Describes the protocol spoken by a transport or a client.
 * <p>
 * The {@code protocol} number is the numeric protocol version used on the wire. Its meaning
 * depends on the {@link #family()}: for Java Edition it is the regular protocol number,
 * for Bedrock Edition it is the Bedrock network version.
 *
 * @param family          the edition of Minecraft
 * @param protocol        the wire protocol number
 * @param minecraftVersion the human-readable Minecraft version, e.g. {@code 1.21.11}
 * @param features        version-specific features supported by this protocol
 */
public record ProtocolVersion(
        @NotNull ProtocolFamily family,
        int protocol,
        @NotNull String minecraftVersion,
        @NotNull Set<ProtocolFeature> features
) {
    public ProtocolVersion {
        Objects.requireNonNull(family, "family");
        Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        features = Set.copyOf(features);
    }

    public ProtocolVersion(@NotNull ProtocolFamily family, int protocol, @NotNull String minecraftVersion) {
        this(family, protocol, minecraftVersion, Set.of());
    }

    /**
     * Returns whether this version supports the given feature.
     */
    public boolean supports(@NotNull ProtocolFeature feature) {
        return features.contains(feature);
    }

    /**
     * Returns whether this version supports a feature with the given name.
     */
    public boolean supports(@NotNull String featureName) {
        for (ProtocolFeature feature : features) {
            if (feature.name().equals(featureName)) return true;
        }
        return false;
    }

    /**
     * Creates a copy of this version with the given features.
     */
    public @NotNull ProtocolVersion withFeatures(@NotNull Set<ProtocolFeature> features) {
        return new ProtocolVersion(family, protocol, minecraftVersion, features);
    }

    @Override
    public String toString() {
        return family.id() + "/" + minecraftVersion + " (" + protocol + ")";
    }
}
