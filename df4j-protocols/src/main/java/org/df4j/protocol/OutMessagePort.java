package org.df4j.protocol;

public interface OutMessagePort<T> {
    void onNext(T message);
    void onComplete();
    void onError(Throwable ex);
}
