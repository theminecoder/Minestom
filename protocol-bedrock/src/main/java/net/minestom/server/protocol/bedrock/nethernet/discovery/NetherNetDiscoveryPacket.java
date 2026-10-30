package net.minestom.server.protocol.bedrock.nethernet.discovery;

import net.minestom.server.protocol.bedrock.packet.BedrockPacketReader;
import net.minestom.server.protocol.bedrock.packet.BedrockPacketWriter;
import org.jetbrains.annotations.NotNull;

/**
 * A NetherNet LAN discovery packet.
 */
public sealed interface NetherNetDiscoveryPacket
        permits NetherNetDiscoveryPacket.Request, NetherNetDiscoveryPacket.Response, NetherNetDiscoveryPacket.Message {

    int TYPE_REQUEST = 0x00;
    int TYPE_RESPONSE = 0x01;
    int TYPE_MESSAGE = 0x02;

    /**
     * The packet type id.
     */
    int type();

    /**
     * Writes the packet data (without the common type/sender header).
     */
    void write(@NotNull BedrockPacketWriter writer);

    /**
     * A client broadcast asking for servers on the LAN.
     */
    record Request() implements NetherNetDiscoveryPacket {
        @Override
        public int type() {
            return TYPE_REQUEST;
        }

        @Override
        public void write(@NotNull BedrockPacketWriter writer) {
            // No payload.
        }
    }

    /**
     * A server's response to a {@link Request}.
     */
    record Response(@NotNull ServerData serverData) implements NetherNetDiscoveryPacket {
        @Override
        public int type() {
            return TYPE_RESPONSE;
        }

        @Override
        public void write(@NotNull BedrockPacketWriter writer) {
            serverData.write(writer);
        }
    }

    /**
     * A WebRTC negotiation message exchanged over the LAN.
     */
    record Message(long recipientId, @NotNull String data) implements NetherNetDiscoveryPacket {
        @Override
        public int type() {
            return TYPE_MESSAGE;
        }

        @Override
        public void write(@NotNull BedrockPacketWriter writer) {
            writer.writeLong(recipientId);
            writer.writeString(data);
        }
    }

    /**
     * The payload advertised by a server on the LAN.
     */
    record ServerData(
            int version,
            @NotNull String serverName,
            @NotNull String levelName,
            int gameType,
            int playerCount,
            int maxPlayerCount,
            boolean editorWorld,
            int transportLayer
    ) {
        public void write(@NotNull BedrockPacketWriter writer) {
            writer.writeByte(version);
            writer.writeString(serverName);
            writer.writeString(levelName);
            writer.writeInt(gameType);
            writer.writeInt(playerCount);
            writer.writeInt(maxPlayerCount);
            writer.writeBoolean(editorWorld);
            writer.writeInt(transportLayer);
        }

        public static @NotNull ServerData read(@NotNull BedrockPacketReader reader) {
            return new ServerData(
                    reader.readByte(),
                    reader.readString(),
                    reader.readString(),
                    reader.readInt(),
                    reader.readInt(),
                    reader.readInt(),
                    reader.readBoolean(),
                    reader.readInt()
            );
        }
    }
}
