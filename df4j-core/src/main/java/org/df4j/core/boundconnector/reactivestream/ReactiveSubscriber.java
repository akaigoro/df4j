package org.df4j.core.boundconnector.reactivestream;

import org.reactivestreams.Subscriber;
import org.df4j.core.boundconnector.messagestream.StreamSubscriber;
import org.reactivestreams.Subscription;

/**
 * receiver of message stream with back pressure
 */
public interface ReactiveSubscriber<T> extends StreamSubscriber<T>, Subscriber<T> {

    @Override
    void onSubscribe(Subscription subscription);

    @Override
    default void onNext(T t) {post(t);}

    @Override
    default void onError(Throwable t) {
        postFailure(t);
    }

    @Override
    default void onComplete() {
        complete();
    }
}
