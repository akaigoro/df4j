package org.df4j.core.core.connector.permitstream;

import org.df4j.core.core.connector.messagescalar.SimpleSubscription;
import org.df4j.core.core.node.AsyncTask;

/**
 * Counting semaphore
 * holds token counter without data.
 * counter can be negative.
 */
public class Semafor extends AsyncTask.Connector implements PermitSubscriber {
    protected final AsyncTask asyncTask;
    private long count = 0;
    protected SimpleSubscription subscription;

    public Semafor(AsyncTask asyncTask) {
        asyncTask.super();
        this.asyncTask = asyncTask;
    }

    public long getCount() {
        return count;
    }

    /** increments resource counter by delta */
    @Override
    public synchronized void release(long delta) {
        if (delta < 0) {
            throw new IllegalArgumentException("resource counter delta must be >= 0");
        }
        long prev = count;
        count+= delta;
        if (prev <= 0 && count > 0 ) {
            turnOn();
        }
    }

    @Override
    public void onSubscribe(SimpleSubscription subscription) {
        this.subscription = subscription;
    }

    /** decrements resource counter by delta */
    protected synchronized void acquire(long delta) {
        if (delta <= 0) {
            throw  new IllegalArgumentException("resource counter delta must be > 0");
        }
        long prev = count;
        count -= delta;
        if (prev > 0 && count <= 0 ) {
            turnOff();
        }
    }

    public synchronized void drainPermits() {
        count = 0;
        turnOff();
    }

    @Override
    public synchronized void purge() {
        acquire(1);
    }
}
