package org.df4j.core.actor;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 *  A {@link PickPoint} able to play role of {@link Subscriber}
 */
public class PickPointSubscriber<T> extends PickPoint<T> implements Subscriber<T> {
    protected Subscription subscription;

    public PickPointSubscriber(int fullCapacity) {
        super(fullCapacity);
    }

    public PickPointSubscriber() {
    }

    @Override
    public void onSubscribe(Subscription s) {
        int requestNumber;
        synchronized (this) {
            this.subscription = s;
            requestNumber = capacity - tokens.size();
        }
        if (requestNumber>0) {
            s.request(requestNumber);
        }
    }

    public void cancel() {
        Subscription s;
        synchronized (this) {
            if (subscription == null) {
                return;
            }
            s = subscription;
            subscription = null;
        }
        s.cancel();
    }
}
