package net.minestom.server.protocol.bedrock.nethernet;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * A WebRTC peer connection managed by a {@link NetherNetPeerFactory}.
 * <p>
 * The peer only deals with the transport: SCTP data channels and ICE candidates. The
 * NetherNet and Bedrock semantics live in {@link NetherNetSession}.
 */
public interface NetherNetPeer extends AutoCloseable {
    /**
     * Sends a complete SCTP message.
     *
     * @param data     the message
     * @param reliable whether to use the reliable data channel
     */
    void send(byte @NotNull [] data, boolean reliable) throws IOException;

    /**
     * Adds a remote ICE candidate received from signaling.
     */
    void addRemoteCandidate(@NotNull String candidate) throws IOException;

    /**
     * The SDP answer to send back to the client (for the server side of the connection).
     */
    @NotNull String sdpAnswer();

    /**
     * Returns whether the peer is still connected.
     */
    boolean isConnected();

    @Override
    void close();
}
