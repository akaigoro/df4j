package org.df4j.core.port;

public interface OutMessagePort<T> {
    void onNext(T message);
    default void onComplete(){}
    default void onError(Throwable ex){}
}
