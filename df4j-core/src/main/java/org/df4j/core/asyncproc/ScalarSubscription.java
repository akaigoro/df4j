package org.df4j.core.asyncproc;

import org.df4j.core.util.linked.Link;

public class ScalarSubscription<T> extends Link<ScalarSubscription<T>> {//implements Subscription {

    protected ScalarSubscriptionQueue<T> listener;
    private ScalarSubscriber subscriber;
    private boolean initialized = false; // todo get rid of it

    public ScalarSubscription(ScalarSubscriptionQueue<T> listener, ScalarSubscriber subscriber) {
        this.listener = listener;
        this.subscriber = subscriber;
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

    public synchronized void cancel() {
        synchronized(this) {
            if (isCancelled()) {
                return;
            }
            subscriber = null;
        }
        listener.cancel((ScalarSubscription<T>) this);
    }

    protected synchronized ScalarSubscriber extractScalarSubscriber() {
        waitInitialized();
        if (isCancelled()) {
            return null;
        } else {
            ScalarSubscriber subscriberLoc = subscriber;
            subscriber = null;
            return subscriberLoc;
        }
    }

    public <T> void onComplete(T value) {
        ScalarSubscriber subscriberLoc;
        synchronized (this) {
            if (isCancelled()) {
                return;
            }
            subscriberLoc = subscriber;
        }
        subscriberLoc.onComplete(value);
    }

    public void onError(Throwable t) {
        ScalarSubscriber subscriberLoc = extractScalarSubscriber();
        if (subscriberLoc == null) {
            return;
        }
        subscriberLoc.onError(t);
    }
}
