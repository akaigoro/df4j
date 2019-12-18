package org.df4j.core.fancy.invoker;

public interface Invoker {

    Object apply(Object... args) throws Exception;

    default boolean returnsValue() {
        return false;
    }
}
