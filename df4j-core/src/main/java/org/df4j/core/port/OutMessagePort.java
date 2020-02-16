package org.df4j.core.port;

public interface OutMessagePort<T> {
    void onNext(T message);
    void onComplete();
    void onError(Throwable ex);
}
/*
	complete(T value)
	completeExceptionally(Throwable ex)
 */
/*
Throws exception	Special value Insert
	add(e)
 */
