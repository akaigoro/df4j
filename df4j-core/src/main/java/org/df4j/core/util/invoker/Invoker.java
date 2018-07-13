package org.df4j.core.util.invoker;

public interface Invoker<R> {
    boolean isEmpty();

    R apply(Object... args) throws Exception;

    default boolean returnsValue() {
        return false;
    }
}
