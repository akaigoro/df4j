package org.df4j.core.portadapter;

import org.df4j.protocol.Flow;
import org.df4j.protocol.FlowSubscription;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class Reactive2FlowSubscriber<T> implements Flow.Subscriber<T>, Subscription {
    private final Subscriber<T> subscriber;
    FlowSubscription subscription;

    public Reactive2FlowSubscriber(Subscriber<T> subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void onSubscribe(FlowSubscription subscription) {
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
