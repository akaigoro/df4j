package org.df4j.core.asyncproc;

import org.df4j.core.ScalarSubscriber;

/**
 * Token storage with standard Subscriber&lt;T&gt; interface.
 * It has place for only one token.
 *
 * @param <T> type of accepted tokens.
 */
public class ScalarInput<T> extends ScalarLock implements ScalarSubscriber<T> {
    protected AsyncProc task;
    /** extracted token */
    protected Throwable completionException;
    protected ScalarSubscription subscription;
    protected T current;

    public ScalarInput(AsyncProc task) {
        super(task);
        this.task = task;
    }

    @Override
    public boolean isParameter() {
        return true;
    }

    @Override
    public T current() {
        return current;
    }

    @Override
    public synchronized void onSubscribe(ScalarSubscription s) {
        this.subscription = s;
    }

    @Override
    public synchronized void onComplete(T message) {
        synchronized(this) {
            if (isCompleted()) {
                return;
            }
            this.current = message;
        }
        complete();
    }

    @Override
    public synchronized void onError(Throwable throwable) {
        if (throwable == null) {
            throw new IllegalArgumentException();
        }
        synchronized(this) {
            if (isCompleted()) {
                return;
            }
            this.completionException = throwable;
        }
        complete();
    }
}
