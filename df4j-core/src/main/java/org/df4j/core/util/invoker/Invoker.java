package org.df4j.core.util.invoker;

public interface Invoker {
    boolean isEmpty();

    Object apply(Object... args) throws Exception;

    default boolean returnsValue() {
        return false;
    }
}
