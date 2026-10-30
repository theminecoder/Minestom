package net.minestom.server.network.player;

/**
 * Implemented by connections which support a negotiated compression threshold.
 * <p>
 * Compression is not universal across protocols (Bedrock, for example, compresses batches
 * differently), so the core only asks connections which advertise this capability.
 */
public interface CompressionConnection {
    /**
     * Enables compression for this connection.
     */
    void startCompression();
}
