# Networking and protocol modules

Minestom separates the game server from the wire protocols it speaks. The root
`:minestom` module contains the protocol-agnostic transport SPI and all game logic;
each edition lives in its own module:

| Module             | Edition                 | Transport                        |
|--------------------|-------------------------|----------------------------------|
| `:minestom`        | none                    | SPI only                         |
| `:protocol-java`   | Java Edition            | TCP (optionally Unix sockets)    |
| `:protocol-bedrock`| Bedrock Edition         | NetherNet (WebRTC data channels) |

A process can host both editions at the same time. The core never references a
concrete protocol implementation, so applications only pay for the modules they
depend on.

## Adding a protocol

```kotlin
dependencies {
    implementation("net.minestom:minestom:<version>")
    implementation("net.minestom:protocol-java:<version>")
    implementation("net.minestom:protocol-bedrock:<version>")
}
```

With the module system, also require the module and let the service loader find
the transport:

```java
module my.server {
    requires net.minestom.server;
    requires net.minestom.server.protocol.java;
    requires net.minestom.server.protocol.bedrock;
}
```

Each protocol module provides a
`net.minestom.server.network.protocol.TransportProvider` in
`META-INF/services`. `TransportManager` discovers these providers with
`ServiceLoader` the first time a transport is requested. Removing a protocol
module from the classpath removes its listener; nothing else has to change.

## Starting transports

Java Edition only (the historical behaviour):

```java
MinecraftServer server = MinecraftServer.init();
server.start("0.0.0.0", 25565);
```

Java Edition and Bedrock Edition in the same process:

```java
MinecraftServer server = MinecraftServer.init();
server.start("0.0.0.0", 25565); // Java, TCP

server.start(ProtocolFamily.BEDROCK,
        new InetSocketAddress("0.0.0.0", BedrockProtocol.NETHERNET_DISCOVERY_PORT));
```

`MinecraftServer.start(ProtocolFamily, SocketAddress)` starts the first
transport, the dispatcher, and the ticker. Later calls only start an additional
transport; the dispatcher and ticker are never started twice.

The bound transports can be inspected through
`MinecraftServer.getTransportManager()`:

```java
TransportManager transports = MinecraftServer.getTransportManager();
transports.transport(ProtocolFamily.JAVA).ifPresent(java -> {
    System.out.println(java.getAddress() + ":" + java.getPort());
});
```

`MinecraftServer.getServer()` and `ServerProcess.server()` return the primary
transport. Java is preferred when both editions are running, otherwise the
first transport is returned. Both methods may return `null` before a transport
has been started.

## Bedrock and NetherNet

Bedrock Edition 26.50+ uses NetherNet, a WebRTC-based transport which replaces
RakNet. This module implements the NetherNet protocol around the parts which do
not need a WebRTC stack:

- LAN discovery on the NetherNet discovery port, including the encrypted
  discovery packet format and HMAC;
- WebRTC signaling (`CONNECTREQUEST`, `CONNECTRESPONSE`, `CANDIDATEADD`);
- session management and SCTP segmentation;
- Bedrock packet ids and reader/writer primitives.

ICE, DTLS, and SCTP are supplied by a pluggable
`net.minestom.server.protocol.bedrock.nethernet.NetherNetPeerFactory`. The JDK
does not ship a WebRTC implementation, so the default factory refuses every
connection. Discovery and signaling still work, but a client cannot finish the
connection until a factory is installed.

Configure the transport before it is started. Because `MinecraftServer.start`
creates the transport internally, create and bind it through the
`TransportManager` when you need to customize it:

```java
TransportManager transports = MinecraftServer.getTransportManager();
NetherNetTransport bedrock = (NetherNetTransport) transports.create(ProtocolFamily.BEDROCK);
bedrock.peerFactory(new MyWebRtcPeerFactory());
bedrock.packetHandler((connection, packetId, reader) -> {
    // Decode and handle one Bedrock packet.
});
bedrock.bind(new InetSocketAddress("0.0.0.0", BedrockProtocol.NETHERNET_DISCOVERY_PORT));
transports.addTransport(bedrock);
bedrock.start();
```

A `NetherNetPeerFactory` answers the client's SDP offer. The returned
`NetherNetPeer` must:

- send outgoing SCTP messages with `send(byte[], reliable)`;
- feed incoming SCTP messages to `NetherNetSession.receiveData(byte[])`;
- close the session when the data channel goes away.

Packets are handed to the configured `BedrockPacketHandler`. Until a Bedrock
login flow installs a handler, incoming packets are ignored and no `Player` is
created.

## Version-specific features

Protocol versions are data, not branches in the transport. A `ProtocolVersion`
contains the protocol family, the numeric wire version, the Minecraft version
string, and a set of `ProtocolFeature`s.

- `JavaProtocol` defines the Java versions and the `JavaProtocol.Feature` enum.
- `BedrockVersion` defines Bedrock releases (`V26_52`, `V26_60`) and their
  feature sets; `BedrockProtocol.VERSION` selects the version served by the
  build.
- `PlayerConnection.getProtocol()` reports the version negotiated for a
  connection, and `supportsProtocolFeature(feature)` checks it.

```java
if (connection.supportsProtocolFeature(JavaProtocol.Feature.KNOWN_PACKS)) {
    // Version-specific behaviour.
}
```

To support a new release, add a `ProtocolVersion` (or `BedrockVersion`) with its
feature set. The transport does not need to change, and core game code can gate
optional behaviour on features instead of on protocol numbers.

## Custom transports

A custom protocol (or a test transport) can skip the module layout and register a
provider directly:

```java
MinecraftServer.getTransportManager().registerProvider(new MyTransportProvider());
```

A later provider for the same `ProtocolFamily` replaces the earlier one, which
also makes it possible to override a built-in transport.
