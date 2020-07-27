package org.df4j.core.port;

import java.util.concurrent.CompletionException;

public interface InpMessagePort<T> {
    boolean isReady();
    T current();
    T poll();
    T remove() throws CompletionException;
    boolean isCompleted();
    Throwable getCompletionException();
    default boolean isCompletedExceptionally() {
        return getCompletionException() != null;
    }
}
