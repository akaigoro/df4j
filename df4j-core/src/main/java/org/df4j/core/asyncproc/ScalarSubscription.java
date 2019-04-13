package org.df4j.core.asyncproc;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class ScalarSubscription<T> extends LinkedSubscription<T, ScalarSubscription<T>> {

    public ScalarSubscription(SubscriptionListener listener, Subscriber subscriber) {
        super(listener, subscriber);
    }
}
