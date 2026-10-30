package net.minestom.server.protocol.bedrock.nethernet.signaling;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * A NetherNet WebRTC negotiation message.
 * <p>
 * Messages are exchanged either over LAN discovery or over the Xbox Live signaling
 * WebSocket. The wire format is {@code MESSAGETYPE CONNECTIONID DATA}.
 *
 * @param type         the message type
 * @param connectionId the connection id assigned by the ICE controller
 * @param data         the type-specific payload (SDP or ICE candidate)
 */
public record NetherNetSignalingMessage(
        @NotNull Type type,
        @NotNull String connectionId,
        @NotNull String data
) {
    public enum Type {
        CONNECT_REQUEST("CONNECTREQUEST"),
        CONNECT_RESPONSE("CONNECTRESPONSE"),
        CANDIDATE_ADD("CANDIDATEADD");

        private final String wireName;

        Type(@NotNull String wireName) {
            this.wireName = wireName;
        }

        public @NotNull String wireName() {
            return wireName;
        }

        public static @NotNull Type fromWireName(@NotNull String name) {
            final String upper = name.toUpperCase(Locale.ROOT);
            for (Type type : values()) {
                if (type.wireName.equals(upper)) return type;
            }
            throw new IllegalArgumentException("Unknown NetherNet signaling message type: " + name);
        }
    }

    /**
     * Creates an SDP offer message.
     */
    public static @NotNull NetherNetSignalingMessage connectRequest(@NotNull String connectionId, @NotNull String sdp) {
        return new NetherNetSignalingMessage(Type.CONNECT_REQUEST, connectionId, sdp);
    }

    /**
     * Creates an SDP answer message.
     */
    public static @NotNull NetherNetSignalingMessage connectResponse(@NotNull String connectionId, @NotNull String sdp) {
        return new NetherNetSignalingMessage(Type.CONNECT_RESPONSE, connectionId, sdp);
    }

    /**
     * Creates an ICE candidate message.
     */
    public static @NotNull NetherNetSignalingMessage candidateAdd(@NotNull String connectionId, @NotNull String candidate) {
        return new NetherNetSignalingMessage(Type.CANDIDATE_ADD, connectionId, candidate);
    }

    /**
     * Parses a signaling message.
     *
     * @throws IllegalArgumentException if the message is malformed
     */
    public static @NotNull NetherNetSignalingMessage parse(@NotNull String message) {
        final int firstSpace = message.indexOf(' ');
        if (firstSpace <= 0) throw new IllegalArgumentException("Malformed NetherNet signaling message: " + message);
        final int secondSpace = message.indexOf(' ', firstSpace + 1);
        if (secondSpace < 0) throw new IllegalArgumentException("Malformed NetherNet signaling message: " + message);

        final Type type = Type.fromWireName(message.substring(0, firstSpace));
        final String connectionId = message.substring(firstSpace + 1, secondSpace);
        final String data = message.substring(secondSpace + 1);
        return new NetherNetSignalingMessage(type, connectionId, data);
    }

    /**
     * Serializes this message to its wire representation.
     */
    public @NotNull String encode() {
        return type.wireName() + " " + connectionId + " " + data;
    }

    @Override
    public String toString() {
        return encode();
    }
}
