package org.df4j.core.asyncproc;

import org.df4j.core.ScalarSubscriber;

/**
 * Token storage with standard Subscriber&lt;T&gt; interface.
 * It has place for only one token.
 *
 * @param <T> type of accepted tokens.
 */
public class ScalarInput<T> extends Transition.Param<T> implements ScalarSubscriber<T> {
    protected AsyncProc task;
    /** extracted token */
    protected Throwable completionException;
    protected ScalarSubscription subscription;

    public ScalarInput(AsyncProc task) {
        task.super();
        this.task = task;
    }

    public synchronized Throwable getCompletionException() {
        return completionException;
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
            if (getCurrent() != null) {
                throw new IllegalStateException("token set already");
            }
            setCurrent(message);
        }
        complete();
    }

    @Override
    public synchronized void onError(Throwable throwable) {
        synchronized(this) {
            if (throwable == null) {
                throw new IllegalArgumentException();
            }
            if (isCompleted()) {
                return;
            }
            this.completionException = throwable;
        }
        complete();
    }
}
