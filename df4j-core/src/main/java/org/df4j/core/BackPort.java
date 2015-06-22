package org.df4j.core;

public interface BackPort<T> {
    void request(int count);
    void takeBack(T token);
}
