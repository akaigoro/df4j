package org.df4j.core.asyncproc;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public abstract class LinkedSubscription<T, S extends LinkedSubscription<T, S>> implements Subscription {
    protected SubscriptionListener<T,S> listener;
    private Subscriber subscriber;
    protected S prev;
    private boolean initialized = false;

    public LinkedSubscription(SubscriptionListener<T,S> listener, Subscriber subscriber) {
        this.listener = listener;
        this.subscriber = subscriber;
    }

    public synchronized boolean isCancelled() {
        return subscriber == null;
    }

    @Override
    public synchronized void request(long n) {
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
        listener.remove((S) this);
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

    public void onNext(T value) {
        Subscriber subscriberLoc;
        synchronized (this) {
            if (isCancelled()) {
                return;
            }
            subscriberLoc = subscriber;
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

    public boolean isLinked() {
        return prev != null;
    }
}
