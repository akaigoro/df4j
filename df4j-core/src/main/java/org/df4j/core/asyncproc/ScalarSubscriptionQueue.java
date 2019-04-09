package org.df4j.core.asyncproc;

import org.reactivestreams.Subscriber;

public class ScalarSubscriptionQueue<T> extends SubscriptionQueue<T, ScalarSubscription<T>> {

    @Override
    public void subscribe(Subscriber<? super T> s) {
        add(new ScalarSubscription(this, s));
    }
}
