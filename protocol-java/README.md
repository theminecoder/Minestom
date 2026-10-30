# protocol-java

Java Edition protocol support for Minestom.

This module contains the TCP (and Unix domain socket) listener, the Java packet
framing and compression/encryption pipeline, and the Java `PlayerConnection`
implementation. It registers a
`net.minestom.server.network.protocol.TransportProvider` through
`META-INF/services`, so adding it to the classpath is enough for
`MinecraftServer.start(...)` to bind a Java listener.

Java protocol constants and version-specific features live in
`net.minestom.server.protocol.java.JavaProtocol`.

See [../docs/networking.md](../docs/networking.md) for startup examples and the
overall module layout.
