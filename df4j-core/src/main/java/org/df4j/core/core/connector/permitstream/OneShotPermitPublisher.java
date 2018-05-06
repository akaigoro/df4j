package org.df4j.core.core.connector.permitstream;

import org.df4j.core.core.connector.messagescalar.SimpleSubscription;

/**
 * allows only one subscriber
 */
public class OneShotPermitPublisher implements PermitPublisher, SimpleSubscription {
    PermitSubscriber subscriber;
    int permitCount;

    @Override
    public void subscribe(PermitSubscriber subscriber) {
        this.subscriber = subscriber;
        subscriber.onSubscribe(this);
        movePermits(subscriber);
    }

    private synchronized void movePermits(PermitSubscriber subscriber) {
        subscriber.release(permitCount);
        permitCount = 0;
    }

    public synchronized void release(long n) {
        if (subscriber == null) {
            permitCount +=n;
        } else {
            subscriber.release(n);
        }
    }

    @Override
    public boolean cancel() {
        subscriber = null;
        return true;
    }
}
