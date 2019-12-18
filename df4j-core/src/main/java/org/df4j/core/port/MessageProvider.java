package org.df4j.core.port;

public interface MessageProvider<T> {
    boolean isReady();
    T current();
    T remove();
}
