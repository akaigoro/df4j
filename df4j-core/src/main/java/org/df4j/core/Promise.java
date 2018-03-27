package org.df4j.core;

public interface Promise<T> {
    void postTo(Port<T> request);
}
