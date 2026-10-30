package net.minestom.server.network.protocol;

import org.jetbrains.annotations.NotNull;

/**
 * A capability that may be present in a given protocol version.
 * <p>
 * Features are intentionally open-ended: the common networking layer only understands the
 * feature's name, while protocol modules are free to define their own version-specific set.
 * This is the extension point used to add behaviour that only exists in some versions of a
 * protocol without leaking protocol-specific types into the core.
 *
 * @see ProtocolVersion#supports(ProtocolFeature)
 */
public interface ProtocolFeature {
    /**
     * The stable name of this feature.
     */
    @NotNull String name();

    /**
     * Creates an ad-hoc feature with the given name.
     * <p>
     * Prefer defining named constants (usually an enum) so features can be compared by
     * identity. This factory is useful for plugin-defined or experimental features.
     *
     * @param name the feature name
     * @return a feature with the given name
     */
    static @NotNull ProtocolFeature named(@NotNull String name) {
        return new NamedProtocolFeature(name);
    }

    record NamedProtocolFeature(@NotNull String name) implements ProtocolFeature {
    }
}
