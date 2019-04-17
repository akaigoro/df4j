package org.df4j.core.actor;

import org.df4j.core.util.linked.Link;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 *
 * @param <T>
 */
public class StreamSubscription<T> extends Link<StreamSubscription<T>> implements Subscription {
    private long requested = 0;
    protected SubscriptionListener<StreamSubscription<T>> listener;
    private Subscriber subscriber;

    public StreamSubscription(SubscriptionListener<StreamSubscription<T>> listener, Subscriber subscriber) {
        this.listener = listener;
        this.subscriber = subscriber;
    }

    public boolean isActive() {
        return requested > 0;
    }

    @Override
    public synchronized void request(long n) {
        if (n <= 0) {
            onError(new IllegalArgumentException());
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
            listener.activate(this);
        }
    }

    public void onNext(T value) {
        synchronized (this) {
            if (isCancelled()) {
                return;
            }
            if (requested == 0) {
                throw new IllegalArgumentException();
            }
            requested--;
        }
        Subscriber subscriberLoc;
        synchronized (this) {
            if (isCancelled()) {
                return;
            }
            subscriberLoc = subscriber;
        }
        subscriberLoc.onNext(value);
    }

    public synchronized boolean isCancelled() {
        return subscriber == null;
    }

    @Override
    public synchronized void cancel() {
        if (isCancelled()) {
            return;
        }
        subscriber = null;
        listener.cancel((StreamSubscription<T>) this);
    }

    protected Subscriber extractSubscriber() {
        synchronized (this) {
      //      waitInitialized();
            if (isCancelled()) {
                return null;
            } else {
                Subscriber subscriberLoc = subscriber;
                subscriber = null;
                return subscriberLoc;
            }
        }
    }

    protected void complete(Throwable ex) {
        Subscriber subscriberLoc;
        synchronized (this) {
            if (isCancelled()) {
                return;
            }
            subscriberLoc = extractSubscriber();
            if (subscriberLoc == null) {
                return;
            }
        }
        if (ex == null) {
            subscriberLoc.onComplete();
        } else {
            subscriberLoc.onError(ex);
        }
    }

    public void onComplete() {
        complete(null);
    }

    public void onError(Throwable ex) {
        complete(ex);
    }
}
