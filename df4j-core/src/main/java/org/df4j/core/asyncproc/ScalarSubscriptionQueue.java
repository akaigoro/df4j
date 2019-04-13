package org.df4j.core.asyncproc;

import org.df4j.core.util.linked.LinkedQueue;

public class ScalarSubscriptionQueue<T> extends LinkedQueue<ScalarSubscription<T>> implements ScalarPublisher<T>{

    public void activate(ScalarSubscription simpleSubscription) {
        throw new UnsupportedOperationException();
    }

    public void subscribe(ScalarSubscriber<? super T> s) {
        ScalarSubscription subscription = new ScalarSubscription(this, s);
        synchronized (this) {
            add(subscription);
        }
        s.onSubscribe(subscription);
        subscription.setInitialized();
    }

    public void onComplete(T value) {
        ScalarSubscription subscription = poll();
        for (; subscription != null; subscription = poll()) {
            subscription.onComplete(value);
        }
    }

    public void onError(Throwable ex) {
        ScalarSubscription subscription = poll();
        for (; subscription != null; subscription = poll()) {
            subscription.onError(ex);
        }
    }
}
