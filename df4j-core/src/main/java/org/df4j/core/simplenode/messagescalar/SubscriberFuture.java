package org.df4j.core.simplenode.messagescalar;

import org.df4j.core.boundconnector.messagescalar.ScalarSubscriber;
import org.df4j.core.boundconnector.messagescalar.SimpleSubscription;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @param <T>
 */
public class SubscriberFuture<T> implements ScalarSubscriber<T>, Future<T> {
    protected SimpleSubscription subscription;
    protected T result = null;
    protected Throwable ex = null;
    protected boolean cancelled = false;

    @Override
    public synchronized void post(T item) {
        this.result = item;
        notifyAll();
    }

    @Override
    public synchronized void postFailure(Throwable throwable) {
        this.ex = throwable;
        notifyAll();
    }

    @Override
    public synchronized void onSubscribe(SimpleSubscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        if (subscription == null) {
            return cancelled;
        }
        SimpleSubscription subscription = this.subscription;
        this.subscription = null;
        cancelled = true;
        boolean result = subscription.cancel();
        return result;
    }

    @Override
    public synchronized boolean isCancelled() {
        return cancelled;
    }

    @Override
    public synchronized boolean isDone() {
        return result != null || ex != null;
    }

    @Override
    public synchronized T get() throws InterruptedException, ExecutionException {
        for (;;) {
            if (result != null) {
                return result;
            } else if (ex != null) {
                throw new ExecutionException(ex);
            } else {
                wait();
            }
        }
    }

    @Override
    public synchronized T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        long end = System.currentTimeMillis()+ unit.toMillis(timeout);
        for (;;) {
            if (result != null) {
                return result;
            } else if (ex != null) {
                throw new ExecutionException(ex);
            } else {
                long timeout1 = end - System.currentTimeMillis();
                if (timeout1 <= 0) {
                    throw new TimeoutException();
                }
                wait(timeout1);
            }
        }
    }
}
