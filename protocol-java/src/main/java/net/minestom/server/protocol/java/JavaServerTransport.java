package net.minestom.server.protocol.java;

import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerFlag;
import net.minestom.server.network.packet.PacketParser;
import net.minestom.server.network.packet.PacketVanilla;
import net.minestom.server.network.packet.client.ClientPacket;
import net.minestom.server.network.protocol.ProtocolFamily;
import net.minestom.server.network.protocol.ProtocolVersion;
import net.minestom.server.network.protocol.ServerTransport;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.EOFException;
import java.io.IOException;
import java.net.*;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The Java Edition TCP transport.
 * <p>
 * Accepts connections on a {@link ServerSocketChannel}, then runs a virtual read thread and
 * a virtual write thread per client. Packet framing, encryption and compression are handled
 * by {@link JavaPlayerConnection}.
 */
public final class JavaServerTransport implements ServerTransport {
    private volatile boolean stop;

    private final PacketParser<ClientPacket> packetParser;

    private ServerSocketChannel serverSocket;
    private SocketAddress socketAddress;
    private String address;
    private int port;

    public JavaServerTransport() {
        this(PacketVanilla.CLIENT_PACKET_PARSER);
    }

    public JavaServerTransport(@NotNull PacketParser<ClientPacket> packetParser) {
        this.packetParser = packetParser;
    }

    @Override
    public @NotNull ProtocolFamily family() {
        return ProtocolFamily.JAVA;
    }

    @Override
    public @NotNull ProtocolVersion protocolVersion() {
        return JavaProtocol.VERSION;
    }

    @Override
    public void bind(@NotNull SocketAddress address) throws IOException {
        final java.net.ProtocolFamily socketFamily;
        switch (address) {
            case InetSocketAddress inetSocketAddress -> {
                this.address = inetSocketAddress.getHostString();
                this.port = inetSocketAddress.getPort();
                final InetAddress resolved = inetSocketAddress.getAddress();
                if (resolved == null) {
                    throw new IOException("Unresolved address: " + inetSocketAddress);
                }
                socketFamily = resolved.getAddress().length == 4 ? StandardProtocolFamily.INET : StandardProtocolFamily.INET6;
            }
            case UnixDomainSocketAddress unixDomainSocketAddress -> {
                this.address = "unix://" + unixDomainSocketAddress.getPath();
                this.port = 0;
                socketFamily = StandardProtocolFamily.UNIX;
            }
            default ->
                    throw new IllegalArgumentException("Address must be an InetSocketAddress or a UnixDomainSocketAddress");
        }

        ServerSocketChannel server = ServerSocketChannel.open(socketFamily);
        try {
            server.bind(address);
        } catch (IOException e) {
            server.close();
            throw e;
        }
        this.serverSocket = server;
        this.socketAddress = address;

        if (address instanceof InetSocketAddress && port == 0) {
            port = server.socket().getLocalPort();
        }
    }

    @Override
    public void start() {
        Check.stateCondition(serverSocket == null, "Transport must be bound before it is started");
        // Use named thread builders for logging
        var readBuilder = Thread.ofVirtual().name("Ms-Java-Socket-Reader-", 0);
        var writeBuilder = Thread.ofVirtual().name("Ms-Java-Socket-Writer-", 0);
        Thread.ofVirtual().name("Ms-Java-Socket-Server").start(() -> {
            while (!stop) {
                try {
                    final SocketChannel client = serverSocket.accept();
                    configureSocket(client);
                    AtomicReference<JavaPlayerConnection> reference = new AtomicReference<>(null);
                    Thread readThread = readBuilder.unstarted(() -> playerReadLoop(reference.get()));
                    Thread writeThread = writeBuilder.unstarted(() -> playerWriteLoop(reference.get()));
                    JavaPlayerConnection connection = new JavaPlayerConnection(client, client.getRemoteAddress(), readThread, writeThread);
                    reference.set(connection);
                    readThread.start();
                    writeThread.start();
                } catch (AsynchronousCloseException ignored) {
                    // We are exiting, bye bye!
                } catch (ClosedChannelException ignored) {
                    break;
                } catch (IOException e) {
                    if (stop) break;
                    MinecraftServer.getExceptionManager().handleException(e);
                }
            }
        });
    }

    private void configureSocket(SocketChannel channel) throws IOException {
        if (channel.getLocalAddress() instanceof InetSocketAddress) {
            Socket socket = channel.socket();
            socket.setSendBufferSize(ServerFlag.SOCKET_SEND_BUFFER_SIZE);
            socket.setReceiveBufferSize(ServerFlag.SOCKET_RECEIVE_BUFFER_SIZE);
            socket.setTcpNoDelay(ServerFlag.SOCKET_NO_DELAY);
            socket.setSoTimeout(ServerFlag.SOCKET_TIMEOUT);
        }
    }

    private void playerReadLoop(JavaPlayerConnection connection) {
        Check.notNull(connection, "connection cannot be null");
        while (!stop) {
            try {
                // Read & process packets
                connection.read(packetParser);
            } catch (ClosedChannelException ignored) {
                break; // We closed the socket during read, just exit.
            } catch (EOFException e) {
                connection.disconnect();
                break;
            } catch (Throwable e) {
                boolean isExpected = e instanceof SocketException && "Connection reset".equals(e.getMessage());
                if (!isExpected) MinecraftServer.getExceptionManager().handleException(e);
                connection.disconnect();
                break;
            }
        }
    }

    private void playerWriteLoop(JavaPlayerConnection connection) {
        Check.notNull(connection, "connection cannot be null");
        while (!stop) {
            try {
                connection.flushSync();
            } catch (ClosedChannelException ignored) {
                break; // We closed the socket during write, just exit.
            } catch (EOFException e) {
                connection.disconnect();
                break;
            } catch (Throwable e) {
                boolean isExpected = e instanceof IOException && "Broken pipe".equals(e.getMessage());
                if (!isExpected) MinecraftServer.getExceptionManager().handleException(e);

                connection.disconnect();
                break;
            }
            if (!connection.isOnline()) {
                try {
                    connection.flushSync();
                    connection.getChannel().close();
                    break;
                } catch (IOException e) {
                    // Disconnect
                    break;
                }
            }
        }
    }

    @Override
    public boolean isOpen() {
        return !stop;
    }

    @Override
    public void stop() {
        this.stop = true;
        try {
            if (serverSocket != null) {
                this.serverSocket.close();
            }

            if (socketAddress instanceof UnixDomainSocketAddress unixDomainSocketAddress) {
                Files.deleteIfExists(unixDomainSocketAddress.getPath());
            }
        } catch (IOException e) {
            MinecraftServer.getExceptionManager().handleException(e);
        }
    }

    @Override
    public @Nullable SocketAddress socketAddress() {
        return socketAddress;
    }

    @Override
    public @Nullable String getAddress() {
        return address;
    }

    @Override
    public int getPort() {
        return port;
    }

    @ApiStatus.Internal
    public @NotNull PacketParser<ClientPacket> packetParser() {
        return packetParser;
    }
}
