package org.df4j.core.asyncproc;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class ScalarSubscription<T> implements Subscription {
    protected SubscriptionListener listener;
    protected Subscriber subscriber;

    protected ScalarSubscription prev;

    public ScalarSubscription(SubscriptionListener listener, Subscriber subscriber) {
        this.listener = listener;
        this.subscriber = subscriber;
    }

    public synchronized boolean isCancelled() {
        return subscriber == null;
    }

    @Override
    public synchronized void request(long n) {
    }

    @Override
    public synchronized void cancel() {
        if (isCancelled()) {
            return;
        }
        listener.remove(this);
        subscriber = null;
    }

    protected Subscriber extractSubscriber() {
        synchronized (this) {
            if (isCancelled()) {
                return null;
            } else {
                Subscriber subscriberLoc = subscriber;
                subscriber = null;
                return subscriberLoc;
            }
        }
    }

    public void onNext(T value) {
        Subscriber subscriberLoc = extractSubscriber();
        if (subscriberLoc == null) {
            return;
        }
        subscriberLoc.onNext(value);
    }

    public void onError(Throwable t) {
        Subscriber subscriberLoc = extractSubscriber();
        if (subscriberLoc == null) {
            return;
        }
        subscriberLoc.onError(t);
    }
}
