package org.df4j.core.core.node.messagescalar;

import org.df4j.core.core.connector.messagescalar.ScalarSubscriber;
import org.df4j.core.core.connector.messagescalar.SimpleSubscription;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @param <T>
 */
public class SimpleFuture<T> implements ScalarSubscriber<T>, Future<T> {
    protected SimpleSubscription subscription;
    protected T value = null;
    protected Throwable ex = null;
    protected boolean cancelled = false;

    @Override
    public synchronized void post(T item) {
        this.value = item;
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
        return value != null || ex != null;
    }

    @Override
    public synchronized T get() throws InterruptedException, ExecutionException {
        for (;;) {
            if (value != null) {
                return value;
            } else if (ex != null) {
                throw new ExecutionException(ex);
            } else {
                wait();
            }
        }
    }

    @Override
    public synchronized T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        for (;;) {
            if (value != null) {
                return value;
            } else if (ex != null) {
                throw new ExecutionException(ex);
            } else {
                wait(unit.toMillis(timeout));
            }
        }
    }
}
