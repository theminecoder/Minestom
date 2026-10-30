package net.minestom.server.network.player;

import net.minestom.server.network.NetworkBuffer;

/**
 * Implemented by connections which can write an already framed buffer without copying it
 * back into a packet object.
 * <p>
 * This is used by the viewable-packet path for Java Edition connections. Protocol modules
 * which use a different framing format simply do not implement this interface, in which
 * case packets are sent individually.
 */
public interface BufferedPacketSender {
    /**
     * Queues a slice of an already framed buffer for sending.
     *
     * @param buffer the buffer containing the framed data
     * @param offset the start offset of the slice
     * @param length the length of the slice
     */
    void sendBufferedPacket(NetworkBuffer buffer, long offset, long length);
}
