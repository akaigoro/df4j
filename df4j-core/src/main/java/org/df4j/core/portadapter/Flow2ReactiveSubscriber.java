package org.df4j.core.portadapter;

import org.df4j.protocol.Flow;
import org.df4j.protocol.FlowSubscription;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class Flow2ReactiveSubscriber<T> implements Subscriber<T>, FlowSubscription {
    private final Flow.Subscriber<T> subscriber;
    Subscription subscription;
    boolean cancelled;

    public Flow2ReactiveSubscriber(Flow.Subscriber<T> subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
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
