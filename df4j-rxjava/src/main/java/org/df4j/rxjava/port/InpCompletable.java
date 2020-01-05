package org.df4j.rxjava.port;

import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import org.df4j.core.dataflow.BasicBlock;

/**
 * One-shot token storage for a Completion token (signal+error).
 * After the token is received, this port stays ready forever.
 */
public class InpCompletable extends BasicBlock.Port implements CompletableObserver {
    protected volatile boolean completed = false;
    private Throwable completionException = null;
    private Disposable subscription;

    /**
     * @param parent {@link BasicBlock} to which this port belongs
     */
    public InpCompletable(BasicBlock parent) {
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
    public void onSubscribe(Disposable subscription) {
        this.subscription = subscription;
    }

    public Throwable getCompletionException() {
        return completionException;
    }

    @Override
    public  void onError(Throwable throwable) {
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

    @Override
    public void onComplete() {
        onError(null);
    }
}
