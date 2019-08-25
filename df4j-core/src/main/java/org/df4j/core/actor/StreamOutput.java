package org.df4j.core.actor;

import org.df4j.core.actor.base.StreamLock;
import org.df4j.core.actor.base.StreamSubscriptionQueue;
import org.df4j.core.asyncproc.AsyncProc;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;

/**
 * Non-blocking analogue of blocking queue.
 * Serves multiple consumers (subscribers)
 *
 * Each message is routed to exactly one subscriber.
 * Has limited buffer for messages.  When the buffer overflows, this {@link StreamOutput#outerLock} blocks.
 *
 * @param <T> the type of transferred messages
 *
 */
public class StreamOutput<T> extends StreamSubscriptionQueue<T> implements Flow.Publisher<T> {

    private final StreamLock outerLock;
    protected int capacity;
    protected Queue<T> tokens;

    public StreamOutput(AsyncProc actor, int capacity) {
        outerLock = new StreamLock(actor);
        outerLock.unblock();
        if (capacity <= 0) {
            throw new IllegalArgumentException();
        }
        this.capacity = capacity;
        this.tokens = new ArrayDeque<>(capacity);
    }

    public StreamOutput(AsyncProc outerActor) {
        this(outerActor, 16);
    }

    @Override
    protected boolean hasNextToken() {
        return !tokens.isEmpty();
    }

    @Override
    protected T nextToken() {
        T t = tokens.poll();
        if (t != null) {
            outerLock.unblock();
        }
        return t;
    }

    public void onNext(T token) {
        if (token == null) {
            throw new NullPointerException();
        }
        locker.lock();
        try {
            if (completionRequested) {
                return;
            }
            if (tokens.size() >= capacity) {
                throw new IllegalStateException("buffer overflow");
            }
            tokens.add(token);
            if (tokens.size() >= capacity) {
                outerLock.block();
            }
            matchingLoop();
        } finally {
            locker.unlock();
        }
    }

    /**
     * Analogue to the method {@link BlockingQueue#take()}
     * @return next value, or null if the stream is completed
     * @throws InterruptedException - if interrupted while waiting
     * @throws ExecutionException - if this stream was completed exceptionally
     */
    public T take() throws InterruptedException, ExecutionException {
        locker.lock();
        try {
            T res = tokens.poll();
            if (res != null) {
                return res;
            }
            Condition cond = locker.newCondition();
            MySubscriber subscriber = new MySubscriber(cond);
            subscribe(subscriber);
            cond.await();
            if (subscriber.thr != null) {
                throw new ExecutionException(subscriber.thr);
            } else {
                return subscriber.res;
            }
        } finally {
            locker.unlock();
        }
    }

    /**
     * Same as the {@link #take()} but with time-out
     * @param timeout - how long to wait before giving up, in units of unit
     * @param unit - a TimeUnit determining how to interpret the timeout parameter
     * @return next value, or null if the stream completed
     * @throws InterruptedException - if interrupted while waiting
     * @throws ExecutionException - if this stream was completed exceptionally
     * @throws TimeoutException - when timeout exceeded
     */
    public synchronized T take(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        locker.lock();
        try {
            T res = tokens.poll();
            if (res != null) {
                return res;
            }
            Condition cond = locker.newCondition();
            MySubscriber subscriber = new MySubscriber(cond);
            subscribe(subscriber);
            if (!cond.await(timeout, unit)) {
                throw new TimeoutException();
            }
            if (subscriber.thr != null) {
                throw new ExecutionException(subscriber.thr);
            } else {
                return subscriber.res;
            }
        } finally {
            locker.unlock();
        }
    }

    private class MySubscriber implements Flow.Subscriber<T> {
        final Condition cond;
        T res;
        Throwable thr;
        private Flow.Subscription subscription;

        private MySubscriber(Condition cond) {
            this.cond = cond;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
        }

        @Override
        public void onNext(T t) {
            locker.lock();
            try {
                res = t;
                cond.signal();
                if (subscription != null) {
                    subscription.cancel();
                    subscription = null;
                }
            } finally {
                locker.unlock();
            }
        }

        @Override
        public void onComplete() {
            onNext(null);
        }

        @Override
        public void onError(Throwable t) {
            locker.lock();
            try {
                thr = t;
                cond.signal();
                if (subscription != null) {
                    subscription.cancel();
                    subscription = null;
                }
            } finally {
                locker.unlock();
            }
        }
    }
}
