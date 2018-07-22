package org.df4j.core.node.messagescalar;

import org.df4j.core.connector.messagescalar.ScalarSubscriber;
import org.df4j.core.connector.messagescalar.SimpleSubscription;

import java.util.concurrent.CancellationException;

/**
 * an unblocking single-shot output connector
 *
 * @param <T>
 */
public class SubscriberPromise<T>
        extends CompletablePromise<T>
        implements ScalarSubscriber<T>
{
    protected SimpleSubscription subscription;
    protected boolean cancelled = false;

    @Override
    public void onSubscribe(SimpleSubscription subscription) {
        if (cancelled) {
            throw new IllegalStateException("cancelled already");
        }
        if (completed) {
            throw new IllegalStateException("completed already");
        }
        this.subscription = subscription;
    }

    /**
     * wrong API design. Future is not a task.
     * @param mayInterruptIfRunning
     * @return
     */
    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        cancelled = true;
        if (subscription != null) {
            subscription.cancel();
        }
        return completeExceptionally(new CancellationException());
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void post(T message) {
        complete(message);
    }

    @Override
    public void postFailure(Throwable ex) {
        completeExceptionally(ex);
    }

    /**
     * Forcibly sets or resets the value subsequently returned by
     * method {@link #get()} and related methods, whether or not
     * already completed. This method is designed for use only in
     * error recovery actions, and even in such situations may result
     * in ongoing dependent completions using established versus
     * overwritten outcomes.
     *
     * @param value the completion value
     */
    public void obtrudeValue(T value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Forcibly causes subsequent invocations of method {@link #get()}
     * and related methods to throw the given exception, whether or
     * not already completed. This method is designed for use only in
     * error recovery actions, and even in such situations may result
     * in ongoing dependent completions using established versus
     * overwritten outcomes.
     *
     * @param ex the exception
     * @throws NullPointerException if the exception is null
     */
    public void obtrudeException(Throwable ex) {
        throw new UnsupportedOperationException();
    }

}
