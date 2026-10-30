package net.minestom.server.protocol.bedrock.nethernet;

import org.jetbrains.annotations.NotNull;

/**
 * Default {@link NetherNetPeerFactory}, used when no WebRTC implementation is installed.
 */
public final class UnavailableNetherNetPeerFactory implements NetherNetPeerFactory {
    private final String reason;

    public UnavailableNetherNetPeerFactory() {
        this("No WebRTC peer factory is installed. Add a libdatachannel/JNI or pure-Java WebRTC implementation and register its NetherNetPeerFactory.");
    }

    public UnavailableNetherNetPeerFactory(@NotNull String reason) {
        this.reason = reason;
    }

    @Override
    public @NotNull NetherNetPeer accept(@NotNull NetherNetSession session) {
        throw new UnsupportedOperationException(reason);
    }
}
