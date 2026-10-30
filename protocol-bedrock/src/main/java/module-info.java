module net.minestom.server.protocol.bedrock {
    requires transitive net.minestom.server;
    requires static org.jetbrains.annotations;
    requires org.slf4j;

    provides net.minestom.server.network.protocol.TransportProvider
            with net.minestom.server.protocol.bedrock.nethernet.NetherNetTransportProvider;

    exports net.minestom.server.protocol.bedrock;
    exports net.minestom.server.protocol.bedrock.packet;
    exports net.minestom.server.protocol.bedrock.nethernet;
    exports net.minestom.server.protocol.bedrock.nethernet.discovery;
    exports net.minestom.server.protocol.bedrock.nethernet.signaling;
}
