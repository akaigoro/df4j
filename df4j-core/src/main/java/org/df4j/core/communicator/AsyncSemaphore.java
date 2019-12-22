package org.df4j.core.communicator;

import org.df4j.protocol.Pulse;

import java.util.LinkedList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *  A {@link Semaphore} extended asynchronous interface to aquire and release permissions.
 *
 * This implementation is unfair: asynchronous clients are served before synchronous (threads blocked in {@link Semaphore#acquire()} method}.
 */
public class AsyncSemaphore extends Semaphore implements Pulse.Publisher {
    private final Lock bblock = new ReentrantLock();
    protected final LinkedList<Pulse.Subscriber> subscribers = new LinkedList<>();

    public AsyncSemaphore(int count) {
        super(count);
    }

    public AsyncSemaphore() {
        this(0);
    }

    /**
     *
     * @param subscriber
     *      the {@link Pulse.Subscriber} that will consume signals from this {@link Pulse.Publisher}
     */
    @Override
    public void subscribe(Pulse.Subscriber subscriber) {
        if (subscriber == null) {
            throw new NullPointerException();
        }
        bblock.lock();
        try {
            if (super.availablePermits() <= 0) {
                subscribers.add(subscriber);
                return;
            }
            if (!super.tryAcquire(1)) {
                throw new RuntimeException("This must not happen, ");
            }
        } finally {
            bblock.unlock();
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
    public boolean unsubscribe(Pulse.Subscriber subscriber) {
       bblock.lock();
       try {
           if (subscriber == null) {
               throw new NullPointerException();
           }
           return subscribers.remove(subscriber);
       } finally {
           bblock.unlock();
       }
    }

    public void release() {
        Pulse.Subscriber subscriber;
        bblock.lock();
        try {
            if (subscribers.size() == 0) {
                super.release();
                return;
            }
            subscriber = subscribers.remove();
        } finally {
            bblock.unlock();
        }
        subscriber.awake();
    }

    public void release(long delta) {
        if (delta <= 0) {
            throw new IllegalArgumentException();
        }
        for (;;) {
            Pulse.Subscriber subscriber;
            bblock.lock();
            try {
                if (subscribers.size() == 0) {
                    release(delta);
                    return;
                }
                subscriber = subscribers.remove();
            } finally {
                bblock.unlock();
            }
            subscriber.awake();
            delta--;
            if (delta == 0) {
                return;
            }
        }
    }
}
