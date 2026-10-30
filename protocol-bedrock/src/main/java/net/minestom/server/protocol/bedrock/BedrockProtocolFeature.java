package net.minestom.server.protocol.bedrock;

import net.minestom.server.network.protocol.ProtocolFeature;

/**
 * Bedrock Edition features which may depend on the negotiated protocol version.
 */
public enum BedrockProtocolFeature implements ProtocolFeature {
    /** The client supports the NetherNet (WebRTC) transport. */
    NETHER_NET,
    /** RakNet is still accepted as a fallback transport. */
    RAKNET,
    /** The client understands sub-chunk request/response packets. */
    SUB_CHUNKING,
    /** The client supports the server-authoritative movement protocol. */
    SERVER_AUTHORITATIVE_MOVEMENT,
    /** The client supports client-side scripting packets. */
    SCRIPTING,
    /** The client supports the new item stack request system. */
    ITEM_STACK_REQUESTS
}
