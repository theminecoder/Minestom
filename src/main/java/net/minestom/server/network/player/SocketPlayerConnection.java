package net.minestom.server.network.player;

import org.jetbrains.annotations.Nullable;

import javax.crypto.SecretKey;
import java.net.SocketAddress;

/**
 * The Java Edition specific part of a player connection.
 * <p>
 * Login handlers live in the core module because they operate on Java Edition packets, but
 * the concrete socket implementation lives in {@code protocol-java}. This abstract class is
 * the narrow bridge between the two; Bedrock connections extend {@link PlayerConnection}
 * directly and never extend this class.
 */
public abstract class SocketPlayerConnection extends PlayerConnection
        implements CompressionConnection, BufferedPacketSender {
    /**
     * The raw username sent by the client during login, before any validation.
     */
    public abstract @Nullable String getLoginUsername();

    /**
     * Sets the raw username sent by the client during login.
     */
    public abstract void UNSAFE_setLoginUsername(String loginUsername);

    /**
     * The profile obtained from a proxy (BungeeCord legacy forwarding), or {@code null}.
     */
    public abstract @Nullable GameProfile gameProfile();

    /**
     * Sets the profile obtained from a proxy.
     */
    public abstract void UNSAFE_setProfile(GameProfile gameProfile);

    /**
     * The nonce used for Mojang authentication.
     */
    public abstract byte[] getNonce();

    /**
     * Sets the nonce used for Mojang authentication.
     */
    public abstract void setNonce(byte[] nonce);

    /**
     * Enables encryption for this connection.
     */
    public abstract void setEncryptionKey(SecretKey secretKey);

    /**
     * Updates the hostname and protocol information sent in the handshake.
     */
    public abstract void refreshServerInformation(@Nullable String serverAddress, int serverPort, int protocolVersion);

    /**
     * Overrides the remote address, used by proxies.
     */
    public abstract void setRemoteAddress(SocketAddress remoteAddress);
}
