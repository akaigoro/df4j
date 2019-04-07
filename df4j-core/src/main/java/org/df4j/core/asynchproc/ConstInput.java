package org.df4j.core.asynchproc;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Token storage with standard Subscriber&lt;T&gt; interface. It has place for only one
 * token, which is never consumed.
 *
 * @param <T>
 *     type of accepted tokens.
 */
public class ConstInput<T> extends Transition.Pin
        implements Subscriber<T>  // to connect to a Publisher
{
    protected Subscription subscription;
    protected boolean cancelled = false;

    /** extracted token */
    protected boolean completed = false;
    protected T current = null;
    protected Throwable exception;

    public ConstInput(Transition baseAsyncProc) {
        baseAsyncProc.super();
    }
    protected boolean isParameter() {
        return true;
    }

    public synchronized T current() {
        if (exception != null) {
            throw new IllegalStateException(exception);
        }
        return current;
    }

    public T getCurrent() {
        return current;
    }

    public Throwable getException() {
        return exception;
    }

    public boolean isCompleted() {
        return completed;
    }

    @Override
    public void onSubscribe(Subscription s) {
        this.subscription = s;
    }

    @Override
    public void onNext(T message) {
        if (message == null) {
            throw new IllegalArgumentException();
        }
        if (isCompleted()) {
            throw new IllegalStateException("token set already");
        }
        current = message;
        unblock();
    }

    @Override
    public void onError(Throwable throwable) {
        if (isCompleted()) {
            throw new IllegalStateException("token set already");
        }
        this.exception = throwable;
    }

    @Override
    public void onComplete() {
        onNext(null);
    }

    public synchronized void cancel() {
        if (subscription == null) {
            return;
        }
        Subscription subscription = this.subscription;
        this.subscription = null;
        cancelled = true;
        subscription.cancel();
    }
}
