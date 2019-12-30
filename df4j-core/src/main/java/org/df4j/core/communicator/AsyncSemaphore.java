package org.df4j.core.communicator;

import org.df4j.protocol.Flow;
import org.df4j.protocol.SignalFlow;

import java.util.LinkedList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *  A {@link Semaphore} extended with asynchronous interface to aquire and release permissions.
 *
 * This implementation is unfair: asynchronous clients are served before synchronous (threads blocked in {@link Semaphore#acquire()} method}.
 */
public class AsyncSemaphore extends Semaphore implements SignalFlow.Publisher {
    private final Lock bblock = new ReentrantLock();
    protected final LinkedList<SignalSubscription> subscriptions = new LinkedList<>();

    public AsyncSemaphore(int count) {
        super(count);
    }

    public AsyncSemaphore() {
        this(0);
    }

    /**
     *
     * @param subscriber
     *      the {@link SignalFlow.Subscriber} that will consume signals from this {@link SignalFlow.Publisher}
     */
    @Override
    public void subscribe(SignalFlow.Subscriber subscriber) {
        if (subscriber == null) {
            throw new NullPointerException();
        }
        bblock.lock();
        try {
            if (super.availablePermits() <= 0) {
                SignalSubscription subscription = new SignalSubscription(subscriber);
                subscriptions.add(subscription);
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

    public void release() {
        SignalFlow.Subscriber subscriber;
        bblock.lock();
        try {
            if (subscriptions.size() == 0) {
                super.release();
                return;
            }
            subscriber = subscriptions.remove().subscriber;
        } finally {
            bblock.unlock();
        }
        subscriber.awake();
    }

    public void release(int permits) {
        if (permits <= 0) {
            throw new IllegalArgumentException();
        }
        bblock.lock();
        try {
        for (;;) {
            if (subscriptions.size() == 0) {
                release(permits);
                return;
            }
            SignalSubscription subscription = subscriptions.remove();
            long delta = Math.min(permits, subscription.remainedRequests);
            subscription.aquired(delta);
            permits-=delta;
            if (permits == 0) {
                return;
            }
        }
        } finally {
            bblock.unlock();
        }
    }

    private class SignalSubscription implements Flow.Subscription {
        SignalFlow.Subscriber subscriber;
        private long remainedRequests;

        private SignalSubscription(SignalFlow.Subscriber subscriber) {
            this.subscriber = subscriber;
            subscriber.onSubscribe(this);
        }

        @Override
        public boolean isCancelled() {
            bblock.lock();
            try {
                return subscriber == null;
            } finally {
                bblock.unlock();
            }
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                throw new IllegalArgumentException();
            }
            bblock.lock();
            try {
                if (subscriber == null) {
                    return;
                }
                if (remainedRequests > 0) {
                    remainedRequests += n;
                    return;
                }
                while (n > 0) {
                    boolean aquired = tryAcquire(1);
                    if (!aquired) {
                        remainedRequests = n;
                        subscriptions.add(this);
                        return;
                    }
                    n--;
                }
                subscriber.awake();
            } finally {
                bblock.unlock();
            }
        }

        void aquired(long delta) {
            remainedRequests -= delta;
            if (remainedRequests > 0) {
                subscriptions.add(this);
                return;
            }
            subscriber.awake();
        }

        @Override
        public void cancel() {
            bblock.lock();
            try {
                if (subscriber == null) {
                    return;
                }
                subscriber = null;
                subscriptions.remove(this);
            } finally {
                bblock.unlock();
            }
        }
    }
}
