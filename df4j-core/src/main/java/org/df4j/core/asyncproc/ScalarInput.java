package org.df4j.core.asyncproc;

import org.df4j.core.asyncproc.base.ScalarLock;
import org.df4j.core.protocols.Disposable;
import org.df4j.core.protocols.Scalar;

/**
 * Token storage with standard Subscriber&lt;T&gt; interface.
 * It has place for only one token.
 *
 * @param <T> type of accepted tokens.
 */
public class ScalarInput<T> extends ScalarLock implements Scalar.Subscriber<T> {
    protected AsyncProc task;
    /** extracted token */
    protected Throwable completionException;
    protected Disposable subscription;
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
    public synchronized void onSubscribe(Disposable s) {
        this.subscription = s;
    }

    @Override
    public void onSuccess(T message) {
        synchronized(this) {
            if (isCompleted()) {
                return;
            }
            this.current = message;
        }
        complete();
    }

    @Override
    public void onError(Throwable throwable) {
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
