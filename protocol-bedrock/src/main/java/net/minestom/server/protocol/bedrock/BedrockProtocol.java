package net.minestom.server.protocol.bedrock;

import net.minestom.server.network.protocol.ProtocolVersion;

/**
 * Bedrock Edition protocol constants.
 */
public final class BedrockProtocol {
    private BedrockProtocol() {
    }

    /**
     * The default Bedrock game port.
     */
    public static final int DEFAULT_PORT = 19132;

    /**
     * Port used by NetherNet LAN discovery.
     */
    public static final int NETHERNET_DISCOVERY_PORT = 7551;

    /**
     * The protocol version served by this build.
     */
    public static final ProtocolVersion VERSION = BedrockVersion.CURRENT.toProtocolVersion();

    /**
     * Returns the Bedrock version descriptor for the given network version.
     * <p>Unknown versions are returned with no optional features, which lets the transport
     * accept them while the game layer decides whether they are compatible.</p>
     */
    public static BedrockVersion version(int protocol) {
        if (protocol == BedrockVersion.V26_60.protocol()) return BedrockVersion.V26_60;
        if (protocol == BedrockVersion.V26_52.protocol()) return BedrockVersion.V26_52;
        return new BedrockVersion(protocol, "unknown", java.util.Set.of());
    }
}
