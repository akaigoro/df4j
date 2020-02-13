package org.df4j.core.port;

import org.df4j.core.dataflow.AsyncProc;
import org.df4j.protocol.Completable;
import org.df4j.protocol.SimpleSubscription;

/**
 * One-shot token storage for a {@link Completion} token (signal+error)
 * After the token is received, this port stays ready forever.
 */
public class InpCompletable extends AsyncProc.Port implements Completable.Observer {
    protected volatile boolean completed = false;
    private Throwable completionException = null;
    private SimpleSubscription subscription;

    /**
     * @param parent {@link AsyncProc} to which this port belongs
     * @param ready initial port state
     */
    public InpCompletable(AsyncProc parent, boolean active) {
        parent.super(false, active);
    }

    public InpCompletable(AsyncProc parent) {
        parent.super(false);
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
    public void onSubscribe(SimpleSubscription subscription) {
        if (completed) {
            subscription.cancel();
            return;
        }
        this.subscription = subscription;
        block();
    }

    public void unsubscribe() {
        plock.lock();
        SimpleSubscription sub;
        try {
            if (subscription == null) {
                return;
            }
            sub = subscription;
            subscription = null;
        } finally {
            plock.unlock();
        }
        sub.cancel();
    }

    public Throwable getCompletionException() {
        return completionException;
    }

    protected  void _onComplete(Object message, Throwable throwable) {
        plock.lock();
        try {
            if (completed) {
                return;
            }
            this.completed = true;
            setValue(message);
            this.completionException = throwable;
            unblock();
        } finally {
            plock.unlock();
        }
    }

    protected void setValue(Object message) {
    }

    @Override
    public  void onError(Throwable throwable) {
        _onComplete(null, throwable);
    }

    @Override
    public void onComplete() {
        _onComplete(null, null);
    }
}
