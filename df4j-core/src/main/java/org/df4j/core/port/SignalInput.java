package org.df4j.core.port;

import org.df4j.core.dataflow.BasicBlock;
import org.df4j.protocol.Signal;

/**
 * Token storage with standard Subscriber&lt;T&gt; interface.
 * It has place for only one token.
 */
public class SignalInput<T> extends BasicBlock.Port implements Signal.Subscriber {
    protected T value;
    protected volatile boolean completed = false;
    private Throwable completionException = null;

    public SignalInput(BasicBlock task) {
        task.super(false);
    }

    public boolean isCompleted() {
        plock.lock();
        try {
            return completed;
        } finally {
            plock.unlock();
        }
    }

    @Override
    public void onComplete() {
        plock.lock();
        try {
            if (completed) {
                return;
            }
            this.completed = true;
            unblock();
        } finally {
            plock.unlock();
        }
    }
}
