package org.df4j.core.actor;

import org.df4j.core.SubscriptionListener;
import org.df4j.core.util.linked.Link;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class StreamSubscription<T> extends Link<StreamSubscription<T>> implements Subscription {
    protected long requested = 0;
    protected SubscriptionListener<StreamSubscription<T>> listener;
    private Subscriber subscriber;
    private boolean initialized = false; // todo get rid of it

    public StreamSubscription(SubscriptionListener listener, Subscriber subscriber) {
        this.listener = (SubscriptionListener<StreamSubscription<T>>) listener;
        this.subscriber = subscriber;
    }

    public long getRequested() {
        return requested;
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
                onError(new IllegalArgumentException());
//                throw new IllegalArgumentException();
                return;
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

    public void onComplete() {
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
        subscriberLoc.onComplete();
    }

    public synchronized boolean isCancelled() {
        return subscriber == null;
    }

    public synchronized void setInitialized() {
        initialized = true;
        notifyAll();
    }

    public synchronized void waitInitialized() {
        while (!initialized) {
            try {
                wait();
            } catch (InterruptedException e) {
                if (subscriber!=null) {
                    subscriber.onError(e);
                }
            }
        }
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
            waitInitialized();
            if (isCancelled()) {
                return null;
            } else {
                Subscriber subscriberLoc = subscriber;
                subscriber = null;
                return subscriberLoc;
            }
        }
    }

    public void onError(Throwable t) {
        Subscriber subscriberLoc = extractSubscriber();
        if (subscriberLoc == null) {
            return;
        }
        subscriberLoc.onError(t);
    }
}
