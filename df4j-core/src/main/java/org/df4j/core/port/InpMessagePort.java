package org.df4j.core.port;

public interface InpMessagePort<T> {
    boolean isReady();
    T current();
    T remove();
}
