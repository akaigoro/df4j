package org.df4j.core.communicator;

import org.df4j.protocol.Flow;
import org.df4j.protocol.FlowSubscription;
import org.reactivestreams.Subscriber;

import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AsyncArrayBlockingQueueBase<T> extends AbstractQueue<T> implements Flow.Publisher<T> {
    protected final Lock qlock = new ReentrantLock();
    protected final Condition hasItems = qlock.newCondition();
    protected final int capacity;
    protected ArrayDeque<T> tokens;
    protected Queue<FlowSubscriptionImpl> subscribers = new LinkedList<FlowSubscriptionImpl>();
    protected Throwable completionException;
    protected volatile boolean completed;

    public AsyncArrayBlockingQueueBase(int capacity) {
        this.capacity = capacity;
        tokens = new ArrayDeque<>(capacity);
    }

    @Override
    public void subscribe(Subscriber subscriber) {
        FlowSubscriptionImpl subscription = new FlowSubscriptionImpl(subscriber);
        subscriber.onSubscribe(subscription);
    }

    /**
     *
     * @param token token to insert
     * @return true if token inserted
     */
    @Override
    public boolean offer(T token) {
        FlowSubscriptionImpl sub;
        qlock.lock();
        try {
            if (subscribers.isEmpty()) {
                boolean success = tokens.offer(token);
                if (success) {
                    hasItemsEvent();
                }
                return success;
            }
            sub = subscribers.remove();
        } finally {
            qlock.unlock();
        }
        sub.onNext(token);
        return true;
    }

    public void hasItemsEvent() {
        hasItems.signalAll();
    }

    @Override
    public T poll() {
        qlock.lock();
        try {
            for (;;) {
                T res = tokens.poll();
                if (res != null) {
                    hasRoomEvent();
                    return res;
                }
                if (completed) {
                    throw new CompletionException(completionException);
                }
                return null;
            }
        } finally {
            qlock.unlock();
        }
    }

    @Override
    public T peek() {
        qlock.lock();
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
            qlock.unlock();
        }
    }

//    @Override
    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        qlock.lock();
        try {
            long millis = unit.toMillis(timeout);
            for (;;) {
                T res = tokens.poll();
                if (res != null) {
                    hasRoomEvent();
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
            qlock.unlock();
        }
    }

    // @Override
    public T take() throws InterruptedException {
        qlock.lock();
        try {
            for (;;) {
                T res = tokens.poll();
                if (res != null) {
                    hasRoomEvent();
                    return res;
                }
                if (completed) {
                    throw new CompletionException(completionException);
                }
                hasItems.await();
            }
        } finally {
            qlock.unlock();
        }
    }

    @Override
    public Iterator<T> iterator() {
        return tokens.iterator();
    }

    @Override
    public int size() {
        return tokens.size();
    }

    public void onError(Throwable cause) {
        qlock.lock();
        try {
            if (completed) {
                return;
            }
            completed = true;
            completionException = cause;
            hasItemsEvent();
            for (;;) {
                FlowSubscriptionImpl sub1 = subscribers.poll();
                if (sub1 == null) {
                    break;
                }
                sub1.onError(cause);
            }
        } finally {
            qlock.unlock();
        }
    }

    public void onComplete() {
        onError(null);
    }

    public abstract void hasRoomEvent();

    protected class FlowSubscriptionImpl implements FlowSubscription {
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
            }
            qlock.lock();
            try {
                if (cancelled) {
                    return;
                }
                if (remainedRequests > 0) {
                    remainedRequests += n;
                    return;
                }
                while (n > 0) {
                    if (tokens.size() == 0) {
                        if (completed) {
                            if (completionException == null) {
                                subscriber.onComplete();
                            } else {
                                subscriber.onError(completionException);
                            }
                        } else {
                            remainedRequests = n;
                            subscribers.add(this);
                        }
                        return;
                    }
                    T value = tokens.remove();
                    subscriber.onNext(value);
                    hasRoomEvent();
                    n--;
                }
            } finally {
                qlock.unlock();
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
                qlock.lock();
                try {
                    subscribers.remove(this);
                } finally {
                    qlock.unlock();
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
        protected boolean onNext(T value) {
            subscriber.onNext(value);
            remainedRequests--;
            return remainedRequests > 0;
        }

        protected void onError(Throwable cause) {
            if (cancelled) {
                return;
            }
            cancelled = true;
            if (cause == null) {
                subscriber.onComplete();
            } else {
                subscriber.onError(cause);
            }
        }
    }
}
