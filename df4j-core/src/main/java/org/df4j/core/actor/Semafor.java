package org.df4j.core.actor;

import org.df4j.core.actor.base.StreamLock;
import org.df4j.core.asyncproc.AsyncProc;

/**
 * Counting semaphore
 * holds token counter without data.
 * counter can be negative.
 */
public class Semafor extends StreamLock implements PermitSubscriber {
    protected final AsyncProc actor;
    private long count = 0;

    public Semafor(AsyncProc actor, int count) {
        super(actor);
        this.actor = actor;
        this.count = count;
        if (count > 0 ) {
            unblock();
        }
    }

    public Semafor(AsyncProc actor) {
        this(actor, 0);
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
        synchronized(this) {
            long prev = count;
            count+= delta;
            if (prev > 0 || count <= 0) {
                return;
            }
        }
        unblock();

    }

    /** decrements resource counter by delta
     *
     * @param delta number of permissions to aquire
     */
    protected synchronized void acquire(long delta) {
        if (delta <= 0) {
            throw  new IllegalArgumentException("resource counter delta must be > 0");
        }
        long prev = count;
        count -= delta;
        if (prev > 0 && count <= 0 ) {
            block();
        }
    }

    public synchronized void drainPermits() {
        count = 0;
        block();
    }

    @Override
    public synchronized boolean moveNext() {
        acquire(1);
        return true;
    }
}
