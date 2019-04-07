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
    protected SubscriptionQueue<T> subscriptions = new SubscriptionQueue<>();

    @Override
    public void onSubscribe(Subscription s) {
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
        SubscriptionQueue.ScalaSubscription subscription = subscriptions.poll();
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
        SubscriptionQueue.ScalaSubscription subscription = subscriptions.poll();
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
     * Should not be used.
     * @param mayInterruptIfRunning
     * @return false
     */
    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public synchronized boolean isCancelled() {
        return false;
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
