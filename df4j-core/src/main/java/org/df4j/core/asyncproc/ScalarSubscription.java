package org.df4j.core.asyncproc;

import org.df4j.core.ScalarSubscriber;
import org.df4j.core.util.linked.Link;

public class ScalarSubscription<T> extends Link<ScalarSubscription<T>> {
    private final ScalarSubscriptionQueue<T> parent;
    protected ScalarSubscriber subscriber;

    public ScalarSubscription(ScalarSubscriptionQueue<T> parent, ScalarSubscriber subscriber) {
        this.subscriber = subscriber;
        this.parent = parent;
    }

    public synchronized boolean isCancelled() {
        return subscriber == null;
    }

    public void cancel() {
        synchronized (this) {
            if (isCancelled()) {
                return;
            }
            subscriber = null;
        }
        parent.remove(this);
    }

    protected synchronized ScalarSubscriber extractScalarSubscriber() {
        if (isCancelled()) {
            return null;
        } else {
            ScalarSubscriber subscriberLoc = subscriber;
            subscriber = null;
            return subscriberLoc;
        }
    }

    public <T> void onComplete(T value) {
        ScalarSubscriber subscriberLoc = extractScalarSubscriber();
        if (subscriberLoc == null) {
            return;
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
