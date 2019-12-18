package org.df4j.core.communicator;

import org.df4j.core.protocol.Completion;

import java.util.LinkedList;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

public class CompletableObservers<S extends Completion.CompletableObserver> extends SignalSubscribers<S> {
    protected Throwable completionException;

    public Throwable getCompletionException() {
        return completionException;
    }

    public void setCompletionException(Throwable completionException) {
        this.completionException = completionException;
    }

    @Override
    public void subscribe(S co) {
        synchronized(this) {
            if (!completed) {
                LinkedList<S> subscribers = getSubscribers();
                subscribers.add(co);
                return;
            }
        }
        if (getCompletionException() == null) {
            co.onComplete();
        } else {
            co.onError(getCompletionException());
        }
    }

    public void onError(Throwable e) {
        LinkedList<S> subs;
        synchronized(this) {
            if (completed) {
                return;
            }
            completed = true;
            setCompletionException(e);
            notifyAll();
            if (subscribers == null) {
                return;
            }
            subs = subscribers;
            subscribers = null;
        }
        for (;;) {
            Completion.CompletableObserver sub = subs.poll();
            if (sub == null) {
                break;
            }
            sub.onError(e);
        }
    }

    @Override
    public synchronized void blockingAwait() {
        super.blockingAwait();
        if (completionException != null) {
            throw new CompletionException(completionException);
        }
    }

    @Override
    public synchronized boolean blockingAwait(long timeout) {
        if (!super.blockingAwait(timeout)) {
            return false;
        }
        if (completionException != null) {
            throw new CompletionException(completionException);
        }
        return true;
    }

    @Override
    public synchronized boolean blockingAwait(long timeout, TimeUnit unit) {
        if (!super.blockingAwait(timeout, unit)) {
            return false;
        }
        if (completionException != null) {
            throw new CompletionException(completionException);
        }
        return true;
    }
}
