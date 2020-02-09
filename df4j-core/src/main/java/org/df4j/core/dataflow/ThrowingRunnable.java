package org.df4j.core.dataflow;

@FunctionalInterface
public interface ThrowingRunnable {
    void run() throws Throwable;
}
