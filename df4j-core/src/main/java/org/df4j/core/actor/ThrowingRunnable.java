package org.df4j.core.actor;

@FunctionalInterface
public interface ThrowingRunnable {
    void run() throws Throwable;
}
