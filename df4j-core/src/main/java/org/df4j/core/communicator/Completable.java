package org.df4j.core.communicator;

import org.df4j.protocol.Completion;

public class Completable extends CompletableObservers<Completion.CompletableObserver> implements Completion.CompletableObserver, Completion.CompletableSource {
    @Override
    protected void doOnError(Completion.CompletableObserver co, Throwable completionException) {
        co.onError(completionException);
    }

    @Override
    protected void doOnComplete(Completion.CompletableObserver co) {
        co.onComplete();
    }
}
