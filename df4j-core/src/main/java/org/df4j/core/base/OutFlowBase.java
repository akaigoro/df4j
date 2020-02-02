package org.df4j.core.base;

import org.df4j.core.util.linked.LinkImpl;
import org.df4j.core.util.linked.LinkedQueue;
import org.df4j.protocol.Flow;
import org.df4j.protocol.FlowSubscription;
import org.reactivestreams.Subscriber;

import java.util.AbstractQueue;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class OutFlowBase<T> extends AbstractQueue<T> implements Flow.Publisher<T> {
    protected final Lock qlock;
    private final Condition hasItems;
    protected final int capacity;
    protected ArrayDeque<T> tokens;
    protected LinkedQueue<FlowSubscriptionImpl> activeSubscribtions = new LinkedQueue<FlowSubscriptionImpl>();
    protected LinkedQueue<FlowSubscriptionImpl> passiveSubscribtions = new LinkedQueue<FlowSubscriptionImpl>();
    protected Throwable completionException;
    protected volatile boolean completed;

    public OutFlowBase(Lock qlock, int bufferCapacity) {
        this.qlock = qlock;
        hasItems = qlock.newCondition();
        this.capacity = bufferCapacity;
        tokens = new ArrayDeque<>(capacity);
    }

    public boolean isCompleted() {
        qlock.lock();
        try {
            return completed && tokens.size() == 0;
        } finally {
            qlock.unlock();
        }
    }

    @Override
    public int size() {
        return tokens.size();
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        FlowSubscriptionImpl subscription = new FlowSubscriptionImpl(subscriber);
        qlock.lock();
        try {
            if (passiveSubscribtions != null) {
                passiveSubscribtions.add(subscription);
            }
        } finally {
            qlock.unlock();
        }
        subscriber.onSubscribe(subscription);
        qlock.lock();
        try {
            if (isCompleted()) {
                subscription.onComplete(completionException);
            }
        } finally {
            qlock.unlock();
        }
    }

    /**
     * how many tokens can be stored in the buffer
     * @return 0 if buffer is full
     */
    private int remainingCapacity() {
        return capacity - tokens.size();
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
            if (completed) {
                return false;
            }
            sub = activeSubscribtions.poll();
            if (sub == null) {
                if (remainingCapacity() == 0) {
                    return false;
                }
                tokens.add(token);
                hasItemsEvent();
                if (remainingCapacity() == 0) {
                    noRoomEvent();
                }
                return true;
            }
        } finally {
            qlock.unlock();
        }
        transferTokens(token, sub);
        return true;
    }

    public void hasItemsEvent() {
        hasItems.signalAll();
    }

    public void transferTokens(T token, FlowSubscriptionImpl sub) {
        LinkedQueue<FlowSubscriptionImpl> asubs;
        LinkedQueue<FlowSubscriptionImpl> psubs;
        for (;;) {
            boolean subIsActive = sub.onNext(token);
            qlock.lock();
            try {
                if (!subIsActive) {
                    if (passiveSubscribtions == null) {
                        sub.onComplete(completionException);
                    } else {
                        passiveSubscribtions.add(sub);
                    }
                    sub = null;
                }
                if (tokens.size() == 0 || (sub == null && activeSubscribtions.size() == 0)) {
                    if (!isCompleted()) {
                        if (sub != null) {
                            if (activeSubscribtions == null) {
                                sub.onComplete(completionException);
                            } else {
                                activeSubscribtions.add(sub);
                            }
                        }
                        return;
                    }
                    asubs = this.activeSubscribtions;
                    this.activeSubscribtions = null;
                    psubs = this.passiveSubscribtions;
                    this.passiveSubscribtions = null;
                    break;
                }
                token = tokens.poll();
                if (remainingCapacity() == 1) {
                    hasRoomEvent();
                }
                if (sub == null) {
                    sub = activeSubscribtions.poll();
                }
            } finally {
                qlock.unlock();
            }
        }
        if (sub != null) {
            sub.onComplete(completionException);
        }
        completAllSubscriptions(asubs, psubs);
    }

    public void completAllSubscriptions(LinkedQueue<FlowSubscriptionImpl> asubs, LinkedQueue<FlowSubscriptionImpl> psubs) {
        if (asubs != null) {
            for (;;) {
                FlowSubscriptionImpl sub = asubs.poll();
                if (sub == null) {
                    break;
                }
                sub.onComplete(completionException);
            }
        }
        if (psubs != null) {
            for (;;) {
                FlowSubscriptionImpl sub = psubs.poll();
                if (sub == null) {
                    break;
                }
                sub.onComplete(completionException);
            }
        }
    }

    public void onError(Throwable cause) {
        LinkedQueue<FlowSubscriptionImpl> asubs;
        LinkedQueue<FlowSubscriptionImpl> psubs;
        qlock.lock();
        try {
            if (completed) {
                return;
            }
            completed = true;
            completionException = cause;
            hasItemsEvent();
            if (tokens.size() > 0) {
                return;
            }
            asubs = this.activeSubscribtions;
            this.activeSubscribtions = null;
            psubs = this.passiveSubscribtions;
            this.passiveSubscribtions = null;
        } finally {
            qlock.unlock();
        }
        completAllSubscriptions(asubs, psubs);
    }

    public void onComplete() {
        onError(null);
    }

    @Override
    public T poll() {
        qlock.lock();
        try {
            for (;;) {
                T res = tokens.poll();
                if (res != null) {
                    hasRoomEvent();
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

    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        qlock.lock();
        try {
            long millis = unit.toMillis(timeout);
            for (;;) {
                T res = tokens.poll();
                if (res != null) {
                    hasRoomEvent();
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

    public T take() throws InterruptedException {
        qlock.lock();
        try {
            for (;;) {
                T res = tokens.poll();
                if (res != null) {
                    hasRoomEvent();
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

    protected class FlowSubscriptionImpl extends LinkImpl<FlowSubscriptionImpl> implements FlowSubscription {
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
            T token;
            slock.lock();
            try {
                if (cancelled) {
                    return;
                }
                remainedRequests += n;
                if (remainedRequests > n) {
                    return;
                }
                qlock.lock();
                try {
                    // remainedRequests was 0, so this subscription was passive
                    if (passiveSubscribtions == null) {
                        return; // port closed;
                    }
                    passiveSubscribtions.remove(this);
                    token = tokens.poll();
                    if (token == null) {
                        activeSubscribtions.add(this);
                        return;
                    }
                } finally {
                    qlock.unlock();
                }
            } finally {
                slock.unlock();
            }
            transferTokens(token, this);
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
                    if (remainedRequests > 0) {
                        if (activeSubscribtions != null) {
                            activeSubscribtions.remove(this);
                        }
                    } else {
                        if (passiveSubscribtions != null) {
                            passiveSubscribtions.remove(this);
                        }
                    }
                } finally {
                    qlock.unlock();
                }
            } finally {
                slock.unlock();
            }
        }

        /**
         * must be unlinked
         * @param token token to pass
         * @return
         */
        private boolean onNext(T token) {
            subscriber.onNext(token);
            slock.lock();
            try {
                remainedRequests--;
                return remainedRequests > 0;
            } finally {
                slock.unlock();
            }
        }

        /**
         * must be unlinked
         * @param cause error
         */
        private void onComplete(Throwable cause) {
            slock.lock();
            try {
                if (cancelled) {
                    return;
                }
                cancelled = true;
            } finally {
                slock.unlock();
            }
            if (cause == null) {
                subscriber.onComplete();
            } else {
                subscriber.onError(cause);
            }
        }

        @Override
        public FlowSubscriptionImpl getItem() {
            return this;
        }
    }
    
    protected void hasRoomEvent() {
    }

    protected void noRoomEvent() {
    }
}
