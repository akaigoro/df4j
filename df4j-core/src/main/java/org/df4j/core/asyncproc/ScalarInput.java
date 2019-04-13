package org.df4j.core.asyncproc;

/**
 * Token storage with standard Subscriber&lt;T&gt; interface.
 * It has place for only one token.
 *
 * @param <T> type of accepted tokens.
 */
public class ScalarInput<T> extends Transition.Pin implements ScalarSubscriber<T> {
    protected AsyncProc task;
    /** extracted token */
    protected T current = null;
    protected Throwable completionException;
    protected ScalarSubscription subscription;

    public ScalarInput(AsyncProc task) {
        task.super();
        this.task = task;
    }

    protected boolean isParameter() {
        return true;
    }

    public synchronized T current() {
        return current;
    }

    public synchronized Throwable getCompletionException() {
        return completionException;
    }

    @Override
    public synchronized void onSubscribe(ScalarSubscription s) {
        this.subscription = s;
        s.request(1);
    }

    @Override
    public synchronized void onComplete(T message) {
        synchronized(this) {
            if (message == null) {
                throw new IllegalArgumentException();
            }
            if (isCompleted()) {
                return;
            }
            if (current != null) {
                throw new IllegalStateException("token set already");
            }
            current = message;
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
