package org.df4j.core.port;

import org.df4j.core.actor.BasicBlock;
import org.df4j.core.protocol.ScalarMessage;

/**
 * Token storage with standard Subscriber&lt;T&gt; interface.
 * It has place for only one token.
 *
 * @param <T> type of accepted tokens.
 */
public class ScalarInput<T> extends BasicBlock.Port implements ScalarMessage.Subscriber<T> {
    protected T value;
    protected volatile boolean completed = false;
    private Throwable completionException = null;

    public ScalarInput(BasicBlock task) {
        task.super(false);
    }

    public synchronized boolean isCompleted() {
        return completed;
    }

    public Throwable getCompletionException() {
        return completionException;
    }

    public synchronized T current() {
        return value;
    }

    @Override
    public void onSuccess(T message) {
        synchronized (this) {
            if (completed) {
                return;
            }
            value = message;
            completed = true;
        }
        synchronized(this) {
            if (unblock()) return;
        }
        decBlocking();
    }

    @Override
    public void onError(Throwable throwable) {
        synchronized(this) {
            if (isCompleted()) {
                return;
            }
            this.completed = true;
            this.completionException = throwable;
            if (unblock()) return;
        }
        decBlocking();
    }
}
