package org.df4j.core.port;

public interface InpMessagePort<T> {
    boolean isReady();
    T current();
    T poll();
    T remove();
    boolean isCompleted();
    Throwable getCompletionException();
    default boolean isCompletedExceptionally() {
        return getCompletionException() != null;
    }
}
