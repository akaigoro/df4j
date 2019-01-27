package org.df4j.core.boundconnector.messagestream;

import org.df4j.core.boundconnector.messagescalar.ScalarSubscriber;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public interface StreamSubscriber<T> extends ScalarSubscriber<T>, Subscriber<T> {

    @Override
    default void onSubscribe(Subscription subscription) {}

    @Override
    default void onNext(T token) {
        post(token);
    }

    @Override
    default void onError(Throwable t) {
        postFailure(t);
    }

    /** closes the message stream */
    void onComplete();
}
