package org.df4j.core.util.asyncmon;

public interface CriticalSection {
    void act(AsyncMonitor monitor);

    default boolean cancel() {
        return false;
    }
}
