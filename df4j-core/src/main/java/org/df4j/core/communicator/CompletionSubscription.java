package org.df4j.core.communicator;

import org.df4j.protocol.Completable;
import org.df4j.protocol.SimpleSubscription;

import java.util.LinkedList;

class CompletionSubscription implements SimpleSubscription {
    private final CompletionI completion;
    Completable.Observer subscriber;
    private boolean cancelled;

    protected CompletionSubscription(CompletionI completion, Completable.Observer subscriber) {
        this.completion = completion;
        this.subscriber = subscriber;
    }

    @Override
    public void cancel() {
        synchronized (completion) {
            if (cancelled) {
                return;
            }
            cancelled = true;
            LinkedList<CompletionSubscription> subscriptions = completion.getSubscriptions();
            if (subscriptions != null) {
                subscriptions.remove(this);
            }
        }
    }

    @Override
    public boolean isCancelled() {
        synchronized (completion) {
            return cancelled;
        }
    }

    void onComplete() {
        Throwable completionException = completion.getCompletionException();
        if (completionException == null) {
            subscriber.onComplete();
        } else {
            subscriber.onError(completionException);
        }
    }
}
