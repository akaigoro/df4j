package org.df4j.core.communicator;

import org.df4j.core.protocol.SignalStream;

import java.util.LinkedList;
import java.util.concurrent.Semaphore;

/**
 * An asynchronous extention to {@link Semaphore}
 * This implementation is unfair: asynchronous clients are served first
 */
public class AsyncSemaphore extends Semaphore implements SignalStream.Publisher {
    protected final LinkedList<SignalStream.Subscriber> subscribers = new LinkedList<>();

    public AsyncSemaphore(int count) {
        super(count);
    }

    public AsyncSemaphore() {
        this(0);
    }

    /**
     *
     * @param subscriber
     *      the {@link SignalStream.Subscriber} that will consume signals from this {@link SignalStream.Publisher}
     */
    @Override
    public void subscribe(SignalStream.Subscriber subscriber) {
        if (subscriber == null) {
            throw new NullPointerException();
        }
        synchronized(this) {
            if (super.availablePermits() <= 0) {
                subscribers.add(subscriber);
                return;
            }
            if (!super.tryAcquire(1)) {
                throw new RuntimeException("This must not happen, ");
            }
        }
        subscriber.awake();
    }

    /**
     *
     * @param subscriber subscriber to be removed from thie wait queue, if present
     * @return {@code true} if the subscriber was removed as a result of this call
     * @throws NullPointerException if the argument is null
     */
   @Override
    public synchronized boolean unsubscribe(SignalStream.Subscriber subscriber) {
       if (subscriber == null) {
           throw new NullPointerException();
       }
        return subscribers.remove(subscriber);
    }

    public void release() {
        SignalStream.Subscriber subscriber;
        synchronized(this) {
            if (subscribers.size() == 0) {
                super.release();
                return;
            }
            subscriber = subscribers.remove();
        }
        subscriber.awake();
    }

    public void release(long delta) {
        if (delta <= 0) {
            throw new IllegalArgumentException();
        }
        for (;;) {
            SignalStream.Subscriber subscriber;
            synchronized(this) {
                if (subscribers.size() == 0) {
                    release(delta);
                    return;
                }
                subscriber = subscribers.remove();
            }
            subscriber.awake();
            delta--;
            if (delta == 0) {
                return;
            }
        }
    }
}
