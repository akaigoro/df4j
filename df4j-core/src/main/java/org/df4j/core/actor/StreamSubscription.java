package org.df4j.core.actor;

import org.df4j.core.asyncproc.ScalarSubscription;
import org.df4j.core.asyncproc.SubscriptionListener;
import org.reactivestreams.Subscriber;

public class StreamSubscription<T> extends ScalarSubscription<T> {

    public StreamSubscription(SubscriptionListener listener, Subscriber subscriber) {
        super(listener, subscriber);
    }

    @Override
    public void onNext(T value) {
        subscriber.onNext(value);
    }

    public void onComplete() {
        Subscriber subscriberLoc = extractSubscriber();
        if (subscriberLoc == null) {
            return;
        }
        subscriberLoc.onComplete();
    }
}
