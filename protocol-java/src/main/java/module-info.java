module net.minestom.server.protocol.java {
    requires transitive net.minestom.server;
    requires static org.jetbrains.annotations;
    requires org.jctools.core;
    requires org.slf4j;

    provides net.minestom.server.network.protocol.TransportProvider
            with net.minestom.server.protocol.java.JavaTransportProvider;

    exports net.minestom.server.protocol.java;
}
