package net.minestom.server.protocol.java;

import net.minestom.server.MinecraftServer;
import net.minestom.server.network.protocol.ProtocolFamily;
import net.minestom.server.network.protocol.ProtocolFeature;
import net.minestom.server.network.protocol.ProtocolVersion;

import java.util.Set;

/**
 * Java Edition protocol constants and version-specific features.
 * <p>
 * This class is the single place where the Java protocol version is defined. Adding support
 * for another Java Edition version means adding a {@link ProtocolVersion} (and, if needed,
 * new {@link Feature features}) here rather than changing the transport.
 */
public final class JavaProtocol {
    private JavaProtocol() {
    }

    /**
     * Features which may be negotiated with a Java Edition client.
     */
    public enum Feature implements ProtocolFeature {
        /** The login state can switch to zlib compression. */
        COMPRESSION,
        /** The connection can be encrypted with AES/CFB8. */
        ENCRYPTION,
        /** The configuration state exists between login and play. */
        CONFIGURATION_STATE,
        /** Registry data can be trimmed through the known-packs exchange. */
        KNOWN_PACKS,
        /** The client supports transfer packets. */
        TRANSFER,
        /** Plugin messages can be exchanged during login. */
        LOGIN_PLUGIN_MESSAGES
    }

    private static final Set<ProtocolFeature> FEATURES = Set.of(
            Feature.COMPRESSION,
            Feature.ENCRYPTION,
            Feature.CONFIGURATION_STATE,
            Feature.KNOWN_PACKS,
            Feature.TRANSFER,
            Feature.LOGIN_PLUGIN_MESSAGES
    );

    /**
     * The version served by this build.
     */
    public static final ProtocolVersion VERSION = new ProtocolVersion(
            ProtocolFamily.JAVA,
            MinecraftServer.protocolVersion(),
            MinecraftServer.versionName(),
            FEATURES
    );

    /**
     * Creates a descriptor for a client which connected with the given protocol number.
     * <p>The feature set is selected for the client's version, which leaves room for
     * per-version differences while the transport stays generic.</p>
     */
    public static ProtocolVersion forClient(int clientProtocol) {
        // All protocol numbers currently supported by this build share the same feature set.
        // When a new version drops a feature, branch on clientProtocol here.
        return new ProtocolVersion(ProtocolFamily.JAVA, clientProtocol, MinecraftServer.versionName(), FEATURES);
    }
}
