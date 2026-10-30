package net.minestom.server.protocol.bedrock;

import net.minestom.server.network.protocol.ProtocolFamily;
import net.minestom.server.network.protocol.ProtocolFeature;
import net.minestom.server.network.protocol.ProtocolVersion;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * A Bedrock Edition protocol version and the features it supports.
 * <p>
 * Bedrock protocol numbers are independent from Java Edition protocol numbers, and the
 * feature set changes frequently. Keeping versions as data makes it possible to add a new
 * release without touching the transport.
 *
 * @param protocol         the Bedrock network version
 * @param minecraftVersion the human-readable release, e.g. {@code 26.60}
 * @param features         version-specific features
 */
public record BedrockVersion(
        int protocol,
        @NotNull String minecraftVersion,
        @NotNull Set<ProtocolFeature> features
) {
    public BedrockVersion {
        features = Set.copyOf(features);
    }

    /** 26.52 - NetherNet is the default transport; RakNet is still available as a fallback. */
    public static final BedrockVersion V26_52 = new BedrockVersion(2193, "26.52", Set.of(
            BedrockProtocolFeature.NETHER_NET,
            BedrockProtocolFeature.SUB_CHUNKING,
            BedrockProtocolFeature.SERVER_AUTHORITATIVE_MOVEMENT,
            BedrockProtocolFeature.SCRIPTING,
            BedrockProtocolFeature.ITEM_STACK_REQUESTS
    ));

    /** 26.60 - RakNet is deprecated; NetherNet is the only supported transport. */
    public static final BedrockVersion V26_60 = new BedrockVersion(2225, "26.60", Set.of(
            BedrockProtocolFeature.NETHER_NET,
            BedrockProtocolFeature.SUB_CHUNKING,
            BedrockProtocolFeature.SERVER_AUTHORITATIVE_MOVEMENT,
            BedrockProtocolFeature.SCRIPTING,
            BedrockProtocolFeature.ITEM_STACK_REQUESTS
    ));

    /**
     * The version served by this build.
     */
    public static final BedrockVersion CURRENT = V26_60;

    public @NotNull ProtocolVersion toProtocolVersion() {
        return new ProtocolVersion(ProtocolFamily.BEDROCK, protocol, minecraftVersion, features);
    }

    public boolean supports(@NotNull ProtocolFeature feature) {
        return features.contains(feature);
    }

    public @NotNull BedrockVersion withFeatures(@NotNull Set<ProtocolFeature> features) {
        return new BedrockVersion(protocol, minecraftVersion, features);
    }
}
