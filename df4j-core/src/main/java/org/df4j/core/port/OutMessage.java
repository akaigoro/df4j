package org.df4j.core.port;

import org.df4j.core.actor.BasicBlock;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletionException;
import org.df4j.protocol.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A Publisher acting like a server
 * unblocked initially
 */
public class OutMessage<T> extends BasicBlock.Port implements Flow.Publisher<T> {
    private final Condition hasItems = plock.newCondition();
    protected Queue<FlowSubscription> subscribers = new LinkedList<FlowSubscription>();
    protected Throwable completionException;
    protected volatile boolean completed;
    protected volatile T value;

    public OutMessage(BasicBlock parent) {
        parent.super(true);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        FlowSubscription subscription = new FlowSubscription(subscriber);
        subscriber.onSubscribe(subscription);
    }

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
            s = subscribers.poll();
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

    public void onError(Throwable t) {
        Queue<FlowSubscription> subs;
        plock.lock();
        try {
            if (completed) {
                return;
            }
            completed = true;
            completionException = t;
            subs = subscribers;
            subscribers = null;
        } finally {
            plock.unlock();
        }
        for (;;) {
            FlowSubscription sub = subs.poll();
            if (sub == null) {
                break;
            }
            sub.onError(t);
        }
    }

    public void onComplete() {
        onError(null);
    }

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

    class FlowSubscription implements Flow.Subscription {
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
                        subscribers.add(this);
                    }
                } else {
                    T res = value;
                    value = null;
                    subscriber.onNext(res);
                    n--;
                    remainedRequests = n;
                    if (remainedRequests > 0) {
                        subscribers.add(this);
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
                subscribers.remove(this);
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
                subscribers.add(this);
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
    }

}
