module org.df4j.core {
    requires  org.df4j.protocols;
    requires java.logging;
    exports org.df4j.core.actor;
    exports org.df4j.core.actor.base;
    exports org.df4j.core.asyncproc;
    exports org.df4j.core.asyncproc.base;
    exports org.df4j.core.util;
    exports org.df4j.core.util.executor;
    exports org.df4j.core.util.invoker;
    exports org.df4j.core.util.linked;
}