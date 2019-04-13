package org.df4j.core.asyncproc;

import org.reactivestreams.Subscriber;

public class ScalarSubscriptionQueue<T> extends SubscriptionQueue<T, ScalarSubscription<T>> {

    public void addLast(Subscriber<? super T> s) {
        ScalarSubscription subscription = new ScalarSubscription(this, s);
        synchronized (this) {
            add(subscription);
        }
        s.onSubscribe(subscription);
        subscription.setInitialized();
    }
}
