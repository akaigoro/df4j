package org.df4j.adapters.reactivestreams;

import org.df4j.core.protocols.Flow;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 *
 * converts org.reactivestreams.Subscriber to a Flow.Subscriber
 *
 * @param <T>
 */
public class Reactive2FlowSubscriber<T> implements Flow.Subscriber<T>, Subscription {
    private final Subscriber<T> subscriber;
    private Flow.Subscription subscription;

    public Reactive2FlowSubscriber(Subscriber<T> subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
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
        subscription.cancel();
    }
}
