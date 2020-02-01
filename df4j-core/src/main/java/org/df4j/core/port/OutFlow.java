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
    protected LinkedQueue<FlowSubscriptionImpl> activeSubscribtions = new LinkedQueue<FlowSubscriptionImpl>();
    protected LinkedQueue<FlowSubscriptionImpl> passiveSubscribtions = new LinkedQueue<FlowSubscriptionImpl>();
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

    public boolean isCompleted() {
        plock.lock();
        try {
            return completed && tokens.size() == 0;
        } finally {
            plock.unlock();
        }
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        FlowSubscriptionImpl subscription = new FlowSubscriptionImpl(subscriber);
        plock.lock();
        try {
            if (passiveSubscribtions != null) {
                passiveSubscribtions.add(subscription);
            }
        } finally {
            plock.unlock();
        }
        subscriber.onSubscribe(subscription);
        plock.lock();
        try {
            if (isCompleted()) {
                subscription.onComplete(completionException);
            }
        } finally {
            plock.unlock();
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
    public void onNext(T token) {
        FlowSubscriptionImpl sub;
        plock.lock();
        try {
            sub = activeSubscribtions.poll();
            if (sub == null) {
                if (remainingCapacity() == 0) {
                    throw new IllegalStateException("buffer overflow");
                }
                tokens.add(token);
                hasItems.signalAll();
                if (remainingCapacity() == 0) {
                    block();
                }
                return;
            }
        } finally {
            plock.unlock();
        }
        transferTokens(token, sub);
    }

    public void transferTokens(T token, FlowSubscriptionImpl sub) {
        LinkedQueue<FlowSubscriptionImpl> asubs;
        LinkedQueue<FlowSubscriptionImpl> psubs;
        for (;;) {
            boolean subIsActive = sub.onNext(token);
            plock.lock();
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
                    unblock();
                }
                if (sub == null) {
                    sub = activeSubscribtions.poll();
                }
            } finally {
                plock.unlock();
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
        plock.lock();
        try {
            if (completed) {
                return;
            }
            completed = true;
            completionException = cause;
            hasItems.signalAll();
            if (tokens.size() > 0) {
                return;
            }
            asubs = this.activeSubscribtions;
            this.activeSubscribtions = null;
            psubs = this.passiveSubscribtions;
            this.passiveSubscribtions = null;
        } finally {
            plock.unlock();
        }
        completAllSubscriptions(asubs, psubs);
    }

    public void onComplete() {
        onError(null);
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
                // remainedRequests was 0, so this subscription was passive
                passiveSubscribtions.remove(this);
                token = tokens.poll();
                if (token == null) {
                    activeSubscribtions.add(this);
                    return;
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
                plock.lock();
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
                    plock.unlock();
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
}
