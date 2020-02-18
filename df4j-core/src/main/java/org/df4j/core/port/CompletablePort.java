package org.df4j.core.port;

import org.df4j.core.dataflow.AsyncProc;

public class CompletablePort extends AsyncProc.Port {
    protected volatile boolean completed = false;
    protected Throwable completionException = null;

    /**
     * @param parent {@link AsyncProc} to which this port belongs
     * @param ready initial port state - port is not blocking the actor's execution
     * @param active initial port state - port partisipates in firing
     */
    public CompletablePort(AsyncProc parent, boolean ready, boolean active) {
        super(parent, ready, active);
    }

    public CompletablePort(AsyncProc parent, boolean ready) {
        super(parent, ready, true);
    }

    public CompletablePort(AsyncProc parent) {
        super(parent, false, true);
    }

    public boolean isCompleted() {
        plock.lock();
        try {
            return completed;
        } finally {
            plock.unlock();
        }
    }

    public Throwable getCompletionException() {
        return completionException;
    }

    protected  void _onComplete(Throwable throwable) {
        plock.lock();
        try {
            if (completed) {
                return;
            }
            this.completed = true;
            this.completionException = throwable;
            unblock();
        } finally {
            plock.unlock();
        }
    }

    public void onComplete() {
        _onComplete(null);
    }

    public void onError(Throwable cause) {
        if (cause == null) {
            throw new NullPointerException();
        }
        _onComplete(cause);
    }
}
