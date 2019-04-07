package org.df4j.core.asynchproc;

import org.reactivestreams.Subscription;

/**
 * Token storage with standard Subscriber&lt;T&gt; interface.
 * It has place for only one token.
 *
 * @param <T> type of accepted tokens.
 */
public class ScalarInput<T> extends Transition.Pin implements org.reactivestreams.Subscriber<T> {
    protected AsyncProc task;
    /** extracted token */
    protected T current = null;
    protected boolean completed = false;
    protected Throwable completionException;
    protected boolean pushback = false; // if true, do not consume
    protected Subscription subscription;

    public ScalarInput(AsyncProc task) {
        task.super();
        this.task = task;
    }

    protected boolean isParameter() {
        return true;
    }

    // ===================== backend

    public boolean hasNext() {
        return !isCompleted();
    }

    public synchronized T next() {
        if (completionException != null) {
            throw new RuntimeException(completionException);
        }
        if (current == null) {
            throw new IllegalStateException();
        }
        T res = current;
        if (pushback) {
            pushback = false;
            // value remains the same, the pin remains turned on
        } else {
            current = null;
            block();
        }
        return res;
    }

    public synchronized T current() {
        return current;
    }

    public synchronized Throwable getCompletionException() {
        return completionException;
    }

    public synchronized boolean isCompleted() {
        return completed;
    }

    @Override
    public synchronized void onSubscribe(Subscription s) {
        this.subscription = s;
        s.request(1);
    }

    @Override
    public synchronized void onNext(T message) {
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
        unblock();
    }

    @Override
    public synchronized void onError(Throwable throwable) {
        onComplete();
        this.completionException = throwable;
    }

    public synchronized void onComplete() {
        if (completed) {
            return;
        }
        completed = true;
        unblock();
    }

}
