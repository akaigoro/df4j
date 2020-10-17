package org.df4j.core.connector;

import org.reactivestreams.Subscription;
import org.df4j.protocol.SignalFlow;

import java.util.LinkedList;
import java.util.concurrent.Semaphore;

/**
 *  A {@link Semaphore} extended with asynchronous interface to aquire and release permissions.
 *
 * This implementation is unfair: asynchronous clients are served before synchronous (threads blocked in {@link Semaphore#acquire()} method}.
 */
public class AsyncSemaphore extends Semaphore implements SignalFlow.Publisher {
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
        synchronized(this) {
            if (super.availablePermits() <= 0) {
                SignalSubscription subscription = new SignalSubscription(subscriber);
                subscriptions.add(subscription);
                return;
            }
            if (!super.tryAcquire(1)) {
                throw new RuntimeException("This must not happen, ");
            }
        }
        subscriber.release();
    }

    public void release() {
        SignalFlow.Subscriber subscriber;
        synchronized(this) {
            if (subscriptions.size() == 0) {
                super.release();
                return;
            }
            subscriber = subscriptions.remove().subscriber;
        }
        subscriber.release();
    }

    public void release(int permits) {
        if (permits <= 0) {
            throw new IllegalArgumentException();
        }
        synchronized(this) {
            for (;;) {
                if (subscriptions.size() == 0) {
                    super.release(permits);
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
        }
    }

    private class SignalSubscription implements Subscription {
        SignalFlow.Subscriber subscriber;
        private long remainedRequests;

        private SignalSubscription(SignalFlow.Subscriber subscriber) {
            this.subscriber = subscriber;
            subscriber.onSubscribe(this);
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                throw new IllegalArgumentException();
            }
            synchronized(this) {
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
            }
            subscriber.release();
        }

        void aquired(long delta) {
            remainedRequests -= delta;
            if (remainedRequests > 0) {
                subscriptions.add(this);
                return;
            }
            subscriber.release();
        }

        @Override
        public synchronized void cancel() {
            if (subscriber == null) {
                return;
            }
            subscriber = null;
            subscriptions.remove(this);
        }
    }
}
