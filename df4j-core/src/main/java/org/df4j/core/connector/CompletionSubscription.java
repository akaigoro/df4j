package org.df4j.core.connector;

import org.df4j.protocol.Completable;
import org.reactivestreams.Subscription;

import java.util.LinkedList;

public class CompletionSubscription implements Subscription {
    private final CompletionI completion;
    Completable.Observer subscriber;
    private boolean cancelled;

    protected CompletionSubscription(CompletionI completion, Completable.Observer subscriber) {
        this.completion = completion;
        this.subscriber = subscriber;
    }

    @Override
    public void request(long n) {
        throw new UnsupportedOperationException();
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

    void onComplete() {
        Throwable completionException = completion.getCompletionException();
        if (completionException == null) {
            subscriber.onComplete();
        } else {
            subscriber.onError(completionException);
        }
    }
}
