package org.df4j.adapters.reactivestreams;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.Flow;

/**
 *
 * converts Flow.Subscriber to a org.reactivestreams.Subscriber
 *
 * @param <T> type of tokens
 */
public class Flow2ReactiveSubscriber<T> implements Subscriber<T>, Flow.Subscription {
    private final Flow.Subscriber<T> subscriber;
    private Subscription subscription;
    private boolean cancelled = false;

    public Flow2ReactiveSubscriber(Flow.Subscriber<T> subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscriber.onSubscribe(this);
    }

    @Override
    public void onError(Throwable t) {
        subscriber.onError(t);
    }

    @Override
    public void onComplete() {
        subscriber.onComplete();
    }

    @Override
    public void onNext(T token) {
        subscriber.onNext(token);
    }

    @Override
    public void request(long n) {
        subscription.request(n);
    }

    @Override
    public void cancel() {
        cancelled = true;
        subscription.cancel();
    }
}
