package org.df4j.core.simplenode.messagescalar;

import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

public class AnyOf<T> extends CompletablePromise<T> implements BiConsumer<T, Throwable> {

    public AnyOf(CompletionStage<? extends T>... sources) {
        for (CompletionStage source: sources) {
            source.whenComplete(this);
        }
    }

    @Override
    public synchronized void accept(T value, Throwable ex) {
        if (!isDone()) {
            return;
        }
        if (ex != null) {
            completeExceptionally(ex);
        } else {
            complete(value);
        }
    }

}
