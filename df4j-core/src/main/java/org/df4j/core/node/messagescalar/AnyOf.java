package org.df4j.core.node.messagescalar;

import org.df4j.core.connector.messagescalar.ScalarSubscriber;

public class AnyOf<T> extends CompletablePromise<T> {

    public AnyOf() {
    }

    public AnyOf(AsyncResult... sources) {
        for (AsyncResult source: sources) {
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
