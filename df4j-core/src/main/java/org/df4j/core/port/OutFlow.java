package org.df4j.core.port;

import org.df4j.core.dataflow.BasicBlock;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletionException;
import org.df4j.protocol.Flow;
import org.df4j.protocol.Subscription;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A passive source of messages (like a server).
 * Unblocked initially.
 * It has room for single message.
 * Blocked when overflow.
 */
public class OutFlow<T> extends BasicBlock.Port implements Flow.Publisher<T> {
    private final Condition hasItems = plock.newCondition();
    protected FlowSubscriptionI<T> subscribers;
    protected Throwable completionException;
    protected volatile boolean completed;
    protected volatile T value;

    /**
     * @param parent {@link BasicBlock} to which this port belongs
     */
    public OutFlow(BasicBlock parent) {
        parent.super(true);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        FlowSubscription subscription = new FlowSubscription(subscriber);
        subscriber.onSubscribe(subscription);
    }

    /**
     * sends the next message in this flow
     * @param t
     */
    public void onNext(T t) {
        FlowSubscription s;
        plock.lock();
        try {
            if (completed) { // this is how CompletableFuture#completeExceptionally works
                return;
            }
            if (!super.isReady()) {
                throw new IllegalStateException();
            }
            s = subscribers==null?null:subscribers.poll();
            if (s == null) {
                value = t;
                super.block();
                hasItems.signalAll();
                return;
            }
        } finally {
            plock.unlock();
        }
        s.onNext(t);
    }

    /**
     * completes this flow exceptionally
     * @param t
     */
    public void onError(Throwable t) {
        FlowSubscriptionI subs;
        plock.lock();
        try {
            if (completed) {
                return;
            }
            completed = true;
            completionException = t;
            subs = subscribers;
            if (subs == null) {
                return;
            }
            subscribers = null;
        } finally {
            plock.unlock();
        }
        subs.onError(t);
    }

    /**
     * completes this flow normally
     */
    public void onComplete() {
        onError(null);
    }

    /**
     * synchronous interface to wait for next message
     * @return next message when availble
     * @throws InterruptedException if this thread was interrupted during waiting
     */
    public  T take() throws InterruptedException {
        plock.lock();
        try {
            T res;
            for (;;) {
                if (completed) {
                    throw new CompletionException(completionException);
                }
                if (value != null) {
                    res = value;
                    value = null;
                    break;
                }
                hasItems.await();
            }
            return res;
        } finally {
            plock.unlock();
        }
    }


    /**
     * synchronous interface to get for next message
     * @return next message if available, null otherwise
     */
    public  T poll() {
        plock.lock();
        try {
            if (completed) {
                throw new CompletionException(completionException);
            }
            if (value != null) {
                T res = value;
                value = null;
                return res;
            } else {
                return null;
            }
        } finally {
            plock.unlock();
        }
    }

    /**
     * synchronous interface to get for next message
     * if message is not available immediately, waits for the specified timeout
     * @return next message if available, null otherwise
     * @param timeout timeout in units
     * @param unit timeout time unit
     * @return next message when availble
     * @throws InterruptedException if this thread was interrupted during waiting
     */
    public  T poll(long timeout, TimeUnit unit) throws InterruptedException {
        plock.lock();
        try {
            if (value != null) {
                return value;
            }
            long millis = unit.toMillis(timeout);
            long targetTime = System.currentTimeMillis() + millis;
            for (;;) {
                if (completed) {
                    throw new CompletionException(completionException);
                }
                if (value != null) {
                    return value;
                }
                if (millis <= 0) {
                    return null;
                }
                hasItems.await(millis, TimeUnit.MILLISECONDS);
                millis = targetTime - System.currentTimeMillis();
            }
        } finally {
            plock.unlock();
        }
    }

    public void addSubscriber(FlowSubscription subscriber) {
        if (subscribers == null) {
            subscribers = subscriber;
        } else {
            subscribers.add(subscriber);
        }
    }

    interface FlowSubscriptionI<T> {

        OutFlow<T>.FlowSubscription poll();

        void add(OutFlow<T>.FlowSubscription flowSubscription);

        void remove(OutFlow<T>.FlowSubscription flowSubscription);

        void onError(Throwable t);
    }

    class FlowSubscriptions implements FlowSubscriptionI<T> {
        private  final Queue<FlowSubscription> subscribers = new LinkedList<FlowSubscription>();

        @Override
        public FlowSubscription poll() {
            return subscribers.poll();
        }

        @Override
        public void add(FlowSubscription flowSubscription) {
            add(flowSubscription);
        }

        @Override
        public void remove(FlowSubscription flowSubscription) {
            remove(flowSubscription);
        }

        @Override
        public void onError(Throwable t) {
            for (;;) {
                FlowSubscription sub = poll();
                if (sub == null) {
                    break;
                }
                sub.onError(t);
            }
        }
    }

    class FlowSubscription implements Subscription, FlowSubscriptionI<T> {
        private final Lock slock = new ReentrantLock();
        protected final Flow.Subscriber subscriber;
        private long remainedRequests = 0;
        private boolean cancelled = false;

        FlowSubscription(Flow.Subscriber subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public  void request(long n) {
            slock.lock();
            try {
                if (n <= 0) {
                    throw new IllegalArgumentException();
                }
                if (cancelled) {
                    return;
                }
                if (remainedRequests > 0) {
                    remainedRequests += n;
                    return;
                }
                if (value == null) {
                    if (completed) {
                        if (completionException == null) {
                            subscriber.onComplete();
                        } else {
                            subscriber.onError(completionException);
                        }
                    } else {
                        remainedRequests = n;
                        addSubscriber(this);
                    }
                } else {
                    T res = value;
                    value = null;
                    subscriber.onNext(res);
                    n--;
                    remainedRequests = n;
                    if (remainedRequests > 0) {
                        addSubscriber(this);
                    }
                    unblock();
                }
            } finally {
                slock.unlock();
            }
        }

        @Override
        public void cancel() {
            plock.lock();
            try {
                if (subscribers!=null) {
                    subscribers.remove(this);
                }
                cancelled = true;
            } finally {
                plock.unlock();
            }
        }

        /**
         *
         * @param value token to pass
         * @return true if can accept more tokens
         */
        public void onNext(T value) {
            subscriber.onNext(value);
            remainedRequests--;
            if (remainedRequests > 0) {
                addSubscriber(this);
            }
        }

        public void onError(Throwable cause) {
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

        @Override
        public FlowSubscription poll() {
            FlowSubscription res = this;
            subscribers = null;
            return res;
        }

        @Override
        public void add(FlowSubscription flowSubscription) {
            subscribers = new FlowSubscriptions();
            subscribers.add(this);
            subscribers.add(flowSubscription);
        }

        @Override
        public void remove(FlowSubscription flowSubscription) {
            subscribers = null;
        }
    }

}
