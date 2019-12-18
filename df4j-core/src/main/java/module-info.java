module org.df4j.core {
    requires  org.df4j.protocols;
    requires java.logging;
    exports org.df4j.core.actor;
    exports org.df4j.core.communicator;
    exports org.df4j.core.port;
    exports org.df4j.core.util;
}