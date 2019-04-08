package org.df4j.core.asyncproc;


import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Universal standalone connector for single value.
 * Has both synchronous and asynchronous interfaces on output end.
 */
public class AsyncResult<T> implements Subscriber<T>, Publisher<T>, Future<T> {
    protected volatile boolean done;
    protected volatile T value;
    protected volatile Throwable completionException;
    protected ScalarSubscriptionQueue<T> subscriptions = new ScalarSubscriptionQueue<>();
    /** in case this instance have supscribed to some other Publisher */
    protected Subscription subscription;

    @Override
    public void onSubscribe(Subscription s) {
        subscription = s;
        s.request(1);
    }

    @Override
    public synchronized void onNext(T t) {
        synchronized(this) {
            if (done) { // this is how CompletableFuture#complete works
                return;
            }
            done = true;
            value = t;
            notifyAll();
        }
        ScalarSubscription subscription = subscriptions.poll();
        for (; subscription != null; subscription = subscriptions.poll()) {
            subscription.onNext(value);
        }
    }

    @Override
    public synchronized void onError(Throwable t) {
        synchronized(this) {
            if (done) { // this is how CompletableFuture#completeExceptionally works
                return;
            }
            done = true;
            completionException = t;
            notifyAll();
        }
        ScalarSubscription subscription = subscriptions.poll();
        for (; subscription != null; subscription = subscriptions.poll()) {
            subscription.onError(completionException);
        }
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        synchronized(this) {
            if (!isDone()) {
                subscriptions.subscribe(s);
                return;
            }
        }
        if (completionException == null) {
            s.onNext(value);
        } else {
            s.onError(completionException);
        }
    }

    /**
     * Cancels subscription, and not the task, as interfece {@link Future} assumes.
     * @param mayInterruptIfRunning not used
     * @return false if was not subscribed
     *         true if was subscribed and subscription cancelled
     */
    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        Subscription subscriptionLoc;
        synchronized(this) {
            subscriptionLoc = subscription;
            if (subscriptionLoc == null) {
                return false;
            }
        }
        subscriptionLoc.cancel();
        return true;
    }

    /**
     *
     * @return true if not subscribed
     */
    @Override
    public synchronized boolean isCancelled() {
        return subscription == null;
    }

    @Override
    public synchronized boolean isDone() {
        return done;
    }

    @Override
    public synchronized T get() throws InterruptedException, ExecutionException {
        while (!isDone()) {
            wait();
        }
        if (completionException == null) {
            return value;
        } else {
            throw new ExecutionException(completionException);
        }
    }

    @Override
    public synchronized T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        long millis = unit.toMillis(timeout);
        long targetTime = System.currentTimeMillis()+ millis;
        while (!isDone()) {
            wait(millis);
            millis = targetTime - System.currentTimeMillis();
            if (millis <= 0) {
                throw new TimeoutException();
            }
        }
        if (completionException == null) {
            return value;
        } else {
            throw new ExecutionException(completionException);
        }
    }

    public void completeExceptionally(Throwable e) {
        onError(e);
    }

    public void complete(T res) {
        onNext(res);
    }

    public void onComplete() {
        onNext(null);
    }

    public static <U> AsyncResult<U> completedResult(U value) {
        AsyncResult<U> result = new AsyncResult<>();
        result.complete(value);
        return result;
    }

}
