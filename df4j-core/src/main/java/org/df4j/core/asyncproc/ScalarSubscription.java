package org.df4j.core.asyncproc;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class ScalarSubscription<T> implements Subscription {
    SubscriptionListener listener;
    protected Subscriber subscriber;
    long requested = 0;

    protected ScalarSubscription prev;

    public ScalarSubscription(SubscriptionListener listener, Subscriber subscriber) {
        this.listener = listener;
        this.subscriber = subscriber;
    }

    public long getRequested() {
        return requested;
    }

    public synchronized boolean isCancelled() {
        return subscriber == null;
    }

    @Override
    public synchronized void request(long n) {
        if (n <= 0) {
            throw new IllegalArgumentException();
        }
        if (isCancelled()) {
            return;
        }
        boolean wasPassive = requested == 0;
        requested += n;
        if (requested < 0) { // overflow
            requested = Long.MAX_VALUE;
        }
        if (wasPassive) {
            listener.serveRequest(this);
        }
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
        Subscriber subscriberLoc;
        synchronized (this) {
            if (isCancelled()) {
                subscriberLoc = null;
            }
            subscriberLoc = subscriber;
            subscriber = null;
        }
        return subscriberLoc;
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
