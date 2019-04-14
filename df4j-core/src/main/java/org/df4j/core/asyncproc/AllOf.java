package org.df4j.core.asyncproc;

import org.df4j.core.asyncproc.ext.AsyncSupplier;

public class AllOf extends AsyncSupplier<Void> {

    public AllOf() {
    }

    public AllOf(ScalarPublisher<?>... sources) {
        for (ScalarPublisher source: sources) {
            registerAsyncResult(source);
        }
    }

    /**
     * blocks this instance from completion until the source is completed.
     *
     * @param source source of completion. successfull or unseccessfull
     */
    public synchronized void registerAsyncResult(ScalarPublisher source) {
        source.subscribe(new Enter());
    }

    public synchronized void registerAsyncResult(AsyncProc... sources) {
        for (AsyncProc source: sources) {
            registerAsyncResult(source.asyncResult());
        }
    }

    @Override
    protected void fire() {
        completeResult(null);
    }

    protected synchronized void postGlobalFailure(Throwable ex) {
        completeResultExceptionally(ex);
    }

    class Enter extends Pin implements ScalarSubscriber<Object> {
        ScalarSubscriptionQueue.ScalarSubscription subscription;

        @Override
        public void onSubscribe(ScalarSubscriptionQueue.ScalarSubscription s) {
            this.subscription = s;
        }

        @Override
        public void onComplete(Object value) {
            super.unblock();
        }

        @Override
        public void onError(Throwable ex) {
            postGlobalFailure(ex);
        }
    }
}
