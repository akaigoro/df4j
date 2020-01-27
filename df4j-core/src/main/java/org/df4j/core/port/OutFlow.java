package org.df4j.core.port;

import org.df4j.core.dataflow.BasicBlock;
import org.df4j.core.util.linked.LinkImpl;
import org.df4j.core.util.linked.LinkedQueue;
import org.df4j.protocol.Flow;
import org.reactivestreams.*;

import java.util.ArrayDeque;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A passive source of messages (like a server).
 * Unblocked initially.
 * It has room for single message.
 * Blocked when overflow.
 *
 * Because of complex logic, it is designed as an Actor itself. However, it still controls firing of the parent actor.
 */

/**
 * is ready when has room to store at least one toke
 * @param <T>
 */
public class OutFlow<T> extends BasicBlock.Port implements Publisher<T>, OutMessagePort<T> {
    private final Condition hasRoom = plock.newCondition();
    private final Condition hasItems = plock.newCondition();
    protected final int capacity;
    protected ArrayDeque<T> tokens;
    protected LinkedQueue<FlowSubscriptionImpl> subscribers = new LinkedQueue<FlowSubscriptionImpl>();
    protected Throwable completionException;
    protected volatile boolean completed;

    public OutFlow(BasicBlock parent, int bufferCapacity) {
        parent.super(true);
        this.capacity = bufferCapacity;
        tokens = new ArrayDeque<>(capacity);
    }
    public OutFlow(BasicBlock parent) {
        this(parent, 16);
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        FlowSubscriptionImpl subscription = new FlowSubscriptionImpl(subscriber);
        subscriber.onSubscribe(subscription);
    }

    private int remainingCapacity() {
        return capacity - tokens.size();
    }

    /**
     *
     * @param token token to insert
     * @return true if token inserted
     */
    @Override
    public void onNext(T token) {
        FlowSubscriptionImpl sub;
        plock.lock();
        try {
            sub = subscribers.poll();
            if (sub == null) {
                if (remainingCapacity() == 0) {
                    throw new IllegalStateException("buffer overflow");
                }
                tokens.add(token);
                if (remainingCapacity() == 0) {
                    block();
                }
                hasItems.signalAll();
                return;
            }
        } finally {
            plock.unlock();
        }
        sub.onNext(token);
    }

    public T poll() {
        plock.lock();
        try {
            for (;;) {
                T res = tokens.poll();
                if (res != null) {
                    unblock();
                    hasRoom.signalAll();
                    return res;
                }
                if (completed) {
                    throw new CompletionException(completionException);
                }
                return null;
            }
        } finally {
            plock.unlock();
        }
    }

    public T peek() {
        plock.lock();
        try {
            for (;;) {
                T res = tokens.peek();
                if (res != null) {
                    return res;
                }
                if (completed) {
                    throw new CompletionException(completionException);
                }
                return null;
            }
        } finally {
            plock.unlock();
        }
    }

    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        plock.lock();
        try {
            long millis = unit.toMillis(timeout);
            for (;;) {
                T res = tokens.poll();
                if (res != null) {
                    unblock();
                    hasRoom.signalAll();
                    return res;
                }
                if (completed) {
                    throw new CompletionException(completionException);
                }
                if (millis <= 0) {
                    return null;
                }
                long targetTime = System.currentTimeMillis() + millis;
                hasItems.await(millis, TimeUnit.MILLISECONDS);
                millis = targetTime - System.currentTimeMillis();
            }
        } finally {
            plock.unlock();
        }
    }

    public T take() throws InterruptedException {
        plock.lock();
        try {
            for (;;) {
                T res = tokens.poll();
                if (res != null) {
                    unblock();
                    hasRoom.signalAll();
                    return res;
                }
                if (completed) {
                    throw new CompletionException(completionException);
                }
                hasItems.await();
            }
        } finally {
            plock.unlock();
        }
    }

    public void onError(Throwable cause) {
        LinkedQueue<FlowSubscriptionImpl> subs;
        plock.lock();
        try {
            if (completed) {
                return;
            }
            completed = true;
            completionException = cause;
            hasItems.signalAll();
            subs = this.subscribers;
            this.subscribers = null;
        } finally {
            plock.unlock();
        }
        for (;;) {
            FlowSubscriptionImpl sub = subs.poll();
            if (sub == null) {
                break;
            }
            sub.onError(cause);
        }
    }

    public void onComplete() {
        onError(null);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    class FlowSubscriptionImpl extends LinkImpl<FlowSubscriptionImpl> implements Flow.Subscription {
        private final Lock slock = new ReentrantLock();
        protected final Subscriber subscriber;
        private long remainedRequests = 0;
        private boolean cancelled = false;

        FlowSubscriptionImpl(Subscriber subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public boolean isCancelled() {
            slock.lock();
            try {
                return cancelled;
            } finally {
                slock.unlock();
            }
        }

        /**
         *
         * @param n the increment of demand
         */
        @Override
        public void request(long n) {
            if (n <= 0) {
                subscriber.onError(new IllegalArgumentException());
                return;
            }
            plock.lock();
            try {
                if (cancelled) {
                    return;
                }
                remainedRequests += n;
                if (remainedRequests > n) {
                    return;
                }
                while (remainedRequests > 0) {
                    T value = tokens.poll();
                    if (value == null) {
                        if (completed) {
                            if (completionException == null) {
                                subscriber.onComplete();
                            } else {
                                subscriber.onError(completionException);
                            }
                            cancelled = true;
                        } else {
                            subscribers.add(this);
                        }
                        return;
                    }
                    subscriber.onNext(value);
                    remainedRequests--;
                    unblock();
                    hasRoom.signalAll();
                }
            } finally {
                plock.unlock();
            }
        }

        @Override
        public void cancel() {
            slock.lock();
            try {
                if (cancelled) {
                    return;
                }
                cancelled = true;
                plock.lock();
                try {
                    if (subscribers != null) {
                        subscribers.remove(this);
                    }
                } finally {
                    plock.unlock();
                }
            } finally {
                slock.unlock();
            }
        }

        /**
         *
         * @param value token to pass
         * @return true if can accept more tokens
         */
        private void onNext(T value) {
            subscriber.onNext(value);
            slock.lock();
            try {
                remainedRequests--;
                if (remainedRequests > 0) {
                    plock.lock();
                    try {
                        subscribers.add(this);
                    } finally {
                        plock.unlock();
                    }
                }
            } finally {
                slock.unlock();
            }
        }

        private void onError(Throwable cause) {
            slock.lock();
            try {
                if (cancelled) {
                    return;
                }
                cancelled = true;
                if (cause == null) {
                    subscriber.onComplete();
                } else {
                    subscriber.onError(cause);
                }
            } finally {
                slock.unlock();
            }
        }

        @Override
        public FlowSubscriptionImpl getItem() {
            return this;
        }
    }
}
