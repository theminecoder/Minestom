package net.minestom.server.protocol.bedrock.nethernet;

import org.jetbrains.annotations.NotNull;

/**
 * Creates the WebRTC side of a NetherNet connection.
 * <p>
 * No JVM implementation of WebRTC (ICE + DTLS + SCTP) ships with the JDK. Minestom keeps
 * that dependency behind this factory so the protocol layer stays usable without native
 * libraries, and so a host can plug in libdatachannel, a pure-Java WebRTC stack, or a
 * managed service.
 *
 * <p>The factory is responsible for answering the client's SDP offer. The returned peer
 * must already be wired to call {@link NetherNetSession#receiveData(byte[])} for incoming
 * SCTP messages and {@link NetherNetSession#close()} when the connection drops.</p>
 */
public interface NetherNetPeerFactory {
    /**
     * Accepts an SDP offer and returns a connected (or connecting) peer.
     *
     * @param session the session which owns the connection
     * @return the peer connection
     * @throws Exception if the WebRTC stack cannot be initialized
     */
    @NotNull NetherNetPeer accept(@NotNull NetherNetSession session) throws Exception;

    /**
     * Creates a factory which refuses every connection.
     * <p>Useful for servers which only advertise NetherNet on the LAN or which want to
     * enable Bedrock support later.</p>
     */
    static @NotNull NetherNetPeerFactory unavailable(@NotNull String reason) {
        return new UnavailableNetherNetPeerFactory(reason);
    }
}
