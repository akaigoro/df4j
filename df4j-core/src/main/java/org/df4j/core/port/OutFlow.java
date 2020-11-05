package org.df4j.core.port;

import org.df4j.core.actor.AsyncProc;
import org.df4j.core.util.LinkedQueue;
import org.df4j.protocol.Flow;
import org.reactivestreams.Subscription;
import org.reactivestreams.Subscriber;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A passive source of messages (like a server).
 * Blocked initially.
 * It has no buffer memory
 * Blocked when overflow.
 * Is ready when has active subscribers
 * @param <T> type of emitted tokens
 */
public class OutFlow<T> extends CompletablePort implements OutMessagePort<T>, Flow.Publisher<T> {
    public static final int DEFAULT_CAPACITY = 16;
    private LinkedQueue<SubscriptionImpl> allSubscribtions = new LinkedQueue<>();
    private LinkedQueue<SubscriptionImpl> activeSubscribtions = new LinkedQueue<>();

    public OutFlow(AsyncProc parent) {
        super(parent);
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        SubscriptionImpl subscription = new SubscriptionImpl(subscriber);
        synchronized(this) {
            allSubscribtions.add(subscription);
        }
        subscriber.onSubscribe(subscription);
        synchronized(this) {
            if (isCompleted()) {
                subscription.onComplete(getCompletionException());
            }
        }
    }

    /**
     *
     * @param token token to insert
     */
    @Override
    public void onNext(T token) {
        if (!offer(token)) {
            throw new IllegalStateException("buffer overflow");
        }
    }

    /**
     *
     * @param token token to insert
     * @return true if token inserted
     */
    public boolean offer(T token) {
        if (token == null) {
            throw new NullPointerException();
        }
        SubscriptionImpl sub;
        synchronized (this) {
            if (completed) {
                return false;
            }
            sub = activeSubscribtions.poll();
            if (sub == null) {
                return false;
            }
            if (activeSubscribtions.size() == 0) {
                block();
            }
        }
        boolean active = sub.onNext(token);
        if (active) {
            synchronized (this) {
                activeSubscribtions.add(sub);
                unblock();
            }

        }
        return true;
    }

    private void completAllSubscriptions() {
        Throwable ex = getCompletionException();
        allSubscribtions.forEach(sub->sub.onComplete(ex));
        allSubscribtions = activeSubscribtions = null;
    }

    public synchronized void _onComplete(Throwable cause) {
        if (completed) {
            return;
        }
        completed = true;
        completionException = cause;
        notifyAll();
        completAllSubscriptions();
    }

    protected class SubscriptionImpl implements Subscription {
        protected final Subscriber subscriber;
        private AtomicLong remainedRequests = new AtomicLong(0);
        private boolean cancelled = false;

        SubscriptionImpl(Subscriber subscriber) {
            this.subscriber = subscriber;
        }

        public synchronized boolean isCancelled() {
            return cancelled;
        }

        /**
         *
         * @param n the increment of demand
         */
        @Override
        public synchronized void request(long n) {
            if (n <= 0) {
                subscriber.onError(new IllegalArgumentException());
                return;
            }
            if (cancelled) {
                return;
            }
            if (remainedRequests.getAndAdd(n) == 0) { // was passive
                activeSubscribtions.add(this);
                unblock();
            }
        }

        @Override
        public synchronized void cancel() {
            cancelled = true;
            if (remainedRequests.get() > 0) {
                activeSubscribtions.remove(this);
            }
            allSubscribtions.remove(this);
        }

        /**
         * must be unlinked
         * @param token token to pass
         */
        private boolean onNext(T token) {
            subscriber.onNext(token);
            return remainedRequests.decrementAndGet() > 0 && !isCancelled();
        }

        /**
         * must be unlinked
         */
        private  void onComplete(Throwable completionException) {
            synchronized(this) {
                cancelled = true;
            }
            if (completionException == null) {
                subscriber.onComplete();
            } else {
                subscriber.onError(completionException);
            }
        }
    }
}
