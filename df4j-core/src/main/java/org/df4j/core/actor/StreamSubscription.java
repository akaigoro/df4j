package org.df4j.core.actor;

import org.df4j.core.asyncproc.ScalarSubscription;
import org.df4j.core.asyncproc.SubscriptionListener;
import org.reactivestreams.Subscriber;

public class StreamSubscription<T> extends ScalarSubscription<T> {
    protected long requested = 0;

    public StreamSubscription(SubscriptionListener listener, Subscriber subscriber) {
        super(listener, subscriber);
    }

    public long getRequested() {
        return requested;
    }

    @Override
    public synchronized void request(long n) {
        if (n <= 0) {
            subscriber.onError(new IllegalArgumentException());
            return;
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
    public void onNext(T value) {
        synchronized (this) {
            if (requested == 0) {
                throw new IllegalArgumentException();
            }
            requested--;
        }
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
