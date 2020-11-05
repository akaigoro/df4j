package org.df4j.core.port;

public interface OutMessagePort<T> {
    void onNext(T message);
    void onComplete();
    void onError(Throwable ex);
}
