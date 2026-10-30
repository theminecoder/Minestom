package net.minestom.server.protocol.bedrock.nethernet.discovery;

import net.minestom.server.protocol.bedrock.packet.BedrockPacketReader;
import net.minestom.server.protocol.bedrock.packet.BedrockPacketWriter;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * Codec for NetherNet LAN discovery packets.
 * <p>
 * Discovery packets are HMAC-SHA256 authenticated and AES-ECB encrypted with a fixed key
 * derived from the byte sequence {@code DE AD BE EF}. The framing is:
 * <pre>
 *   HMAC-SHA256 (32 bytes)
 *   AES-ECB( uint16 length | uint16 type | uint64 sender | 8-byte padding | payload )
 * </pre>
 * The HMAC is computed over the plaintext length-prefixed frame, exactly as described by
 * the NetherNet specification.
 */
public final class NetherNetDiscoveryCodec {
    private static final byte[] KEY = sha256(new byte[]{(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF});
    private static final byte[] HMAC_KEY = KEY;
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";

    private NetherNetDiscoveryCodec() {
    }

    /**
     * Encodes a discovery packet.
     *
     * @param senderId the sender's session id
     * @param packet   the packet
     * @return the authenticated, encrypted packet
     */
    public static byte @NotNull [] encode(long senderId, @NotNull NetherNetDiscoveryPacket packet) {
        final BedrockPacketWriter bodyWriter = new BedrockPacketWriter();
        bodyWriter.writeShort(packet.type());
        bodyWriter.writeLong(senderId);
        bodyWriter.writeLong(0L); // 8 bytes of padding
        packet.write(bodyWriter);
        final byte[] body = bodyWriter.toByteArray();

        final BedrockPacketWriter frameWriter = new BedrockPacketWriter(body.length + 2);
        frameWriter.writeShort(body.length);
        frameWriter.writeBytes(body);
        final byte[] frame = frameWriter.toByteArray();

        try {
            final byte[] mac = hmac(frame);
            final Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY, "AES"));
            final byte[] encrypted = cipher.doFinal(frame);

            final byte[] result = new byte[mac.length + encrypted.length];
            System.arraycopy(mac, 0, result, 0, mac.length);
            System.arraycopy(encrypted, 0, result, mac.length, encrypted.length);
            return result;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to encode NetherNet discovery packet", e);
        }
    }

    /**
     * Decodes a discovery packet.
     *
     * @param data the authenticated, encrypted packet
     * @return the decoded packet
     * @throws IllegalArgumentException if authentication fails or the packet is malformed
     */
    public static @NotNull Decoded decode(byte @NotNull [] data) {
        if (data.length < 33) throw new IllegalArgumentException("Discovery packet is too short");
        final byte[] mac = Arrays.copyOfRange(data, 0, 32);
        final byte[] encrypted = Arrays.copyOfRange(data, 32, data.length);
        try {
            final Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY, "AES"));
            final byte[] frame = cipher.doFinal(encrypted);
            if (!MessageDigest.isEqual(mac, hmac(frame))) {
                throw new IllegalArgumentException("Invalid NetherNet discovery HMAC");
            }
            final BedrockPacketReader frameReader = new BedrockPacketReader(frame);
            final int declaredLength = frameReader.readShort();
            final byte[] body = frameReader.readBytes(declaredLength);
            final BedrockPacketReader reader = new BedrockPacketReader(body);
            final int type = reader.readShort();
            final long senderId = reader.readLong();
            reader.readLong(); // padding

            final NetherNetDiscoveryPacket packet = switch (type) {
                case NetherNetDiscoveryPacket.TYPE_REQUEST -> {
                    if (reader.remainingBytes() != 0) {
                        throw new IllegalArgumentException("Trailing bytes in discovery request");
                    }
                    yield new NetherNetDiscoveryPacket.Request();
                }
                case NetherNetDiscoveryPacket.TYPE_RESPONSE ->
                        new NetherNetDiscoveryPacket.Response(NetherNetDiscoveryPacket.ServerData.read(reader));
                case NetherNetDiscoveryPacket.TYPE_MESSAGE ->
                        new NetherNetDiscoveryPacket.Message(reader.readLong(), reader.readString());
                default -> throw new IllegalArgumentException("Unknown NetherNet discovery packet type " + type);
            };
            return new Decoded(senderId, packet);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Unable to decode NetherNet discovery packet", e);
        }
    }

    /**
     * Builds a response to a discovery request.
     */
    public static byte @NotNull [] response(long serverId, @NotNull NetherNetDiscoveryPacket.ServerData serverData) {
        return encode(serverId, new NetherNetDiscoveryPacket.Response(serverData));
    }

    private static byte @NotNull [] hmac(byte[] data) throws GeneralSecurityException {
        final Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(HMAC_KEY, HMAC_ALGORITHM));
        return mac.doFinal(data);
    }

    private static byte @NotNull [] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    /**
     * A decoded discovery packet and its sender id.
     */
    public record Decoded(long senderId, @NotNull NetherNetDiscoveryPacket packet) {
    }
}
