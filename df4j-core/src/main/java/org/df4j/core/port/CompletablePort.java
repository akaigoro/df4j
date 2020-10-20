package org.df4j.core.port;

import org.df4j.core.actor.AsyncProc;
import org.df4j.core.actor.TransitionHolder;

public class CompletablePort extends AsyncProc.Port {
    protected volatile boolean completed = false;
    protected Throwable completionException = null;

    /**
     * @param parent {@link AsyncProc} to which this port belongs
     * @param ready initial port state - port is not blocking the actor's execution
     */
    public CompletablePort(AsyncProc parent, boolean ready) {
        super(parent, ready);
    }

    public CompletablePort(TransitionHolder parent) {
        super(parent, false);
    }

    public boolean isCompleted() {
        synchronized(transition) {
            return completed;
        }
    }

    public Throwable getCompletionException() {
        return completionException;
    }

    protected  void _onComplete(Throwable throwable) {
        synchronized(transition) {
            if (completed) {
                return;
            }
            this.completed = true;
            this.completionException = throwable;
            unblock();
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
