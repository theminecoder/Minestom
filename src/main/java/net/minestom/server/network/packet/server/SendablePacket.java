package net.minestom.server.network.packet.server;

import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a packet that can be sent to a {@link PlayerConnection}.
 * <p>
 * This interface is intentionally open so protocol modules can add their own packet types
 * (for example {@code BedrockServerPacket}). Java Edition packets are represented by
 * {@link ServerPacket} and its wrappers.
 */
public interface SendablePacket {

    static @Nullable ServerPacket extractServerPacket(ConnectionState state, SendablePacket packet) {
        return switch (packet) {
            case ServerPacket serverPacket -> serverPacket;
            case CachedPacket cachedPacket -> cachedPacket.packet(state);
            case FramedPacket framedPacket -> framedPacket.packet();
            case LazyPacket lazyPacket -> lazyPacket.packet();
            case BufferedPacket ignored -> null;
            default -> null;
        };
    }
}
