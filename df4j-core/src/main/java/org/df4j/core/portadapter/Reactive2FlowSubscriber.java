package org.df4j.core.portadapter;

import org.df4j.protocol.Flow;
import org.reactivestreams.Subscriber;

public class Reactive2FlowSubscriber<T> implements Flow.Subscriber<T>, org.reactivestreams.Subscription {
    private final Subscriber<T> subscriber;
    Flow.Subscription subscription;

    public Reactive2FlowSubscriber(Subscriber<T> subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscriber.onSubscribe(this);
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
        subscription.cancel();
    }
}
