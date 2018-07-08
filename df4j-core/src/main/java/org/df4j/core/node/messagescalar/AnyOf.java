package org.df4j.core.node.messagescalar;

import org.df4j.core.connector.messagescalar.CompletablePromise;
import org.df4j.core.connector.messagescalar.ScalarSubscriber;

public class AnyOf<T> extends CompletablePromise<T> {

    public AnyOf() {
    }

    public AnyOf(CompletablePromise<? extends T>... sources) {
        for (CompletablePromise source: sources) {
            source.subscribe(new Enter());
        }
    }

    class Enter implements ScalarSubscriber<T> {
        @Override
        public void post(T value) {
            synchronized (AnyOf.this) {
                if (!isDone()) {
                    complete(value);
                }
            }
        }

        @Override
        public void postFailure(Throwable ex) {
            synchronized (AnyOf.this) {
                if (!isDone()) {
                    completeExceptionally(ex);
                }
            }
        }
    }

}
