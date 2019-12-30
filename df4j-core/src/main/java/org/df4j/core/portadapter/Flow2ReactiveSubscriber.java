package org.df4j.core.portadapter;

import org.df4j.protocol.Flow;
import org.reactivestreams.Subscriber;

public class Flow2ReactiveSubscriber<T> implements Subscriber<T>, Flow.Subscription {
    private final Flow.Subscriber<T> subscriber;
    org.reactivestreams.Subscription subscription;
    boolean cancelled;

    public Flow2ReactiveSubscriber(Flow.Subscriber<T> subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void onSubscribe(org.reactivestreams.Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public void onNext(T message) {
        subscriber.onNext(message);
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
    public void request(long n) {
        subscription.request(n);
    }

    @Override
    public void cancel() {
        cancelled = true;
        subscription.cancel();
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }
}
