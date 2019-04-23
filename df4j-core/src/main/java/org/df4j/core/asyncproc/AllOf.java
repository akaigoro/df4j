package org.df4j.core.asyncproc;

import org.df4j.core.ScalarPublisher;
import org.df4j.core.ScalarSubscriber;
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

    /**
     * does not blocks this instance from completion.
     * Used to collect possible exceptions only
     *
     * @param source source of errors
     */
    public synchronized void registerAsyncDaemon(ScalarPublisher source) {
        source.subscribe(new DaemonEnter());
    }

    @Override
    protected void fire() {
        completeResult(null);
    }

    protected synchronized void postGlobalFailure(Throwable ex) {
        completeResultExceptionally(ex);
    }

    class Enter extends ScalarLock implements ScalarSubscriber<Object> {
        ScalarSubscription subscription;

        public Enter() {
            super(AllOf.this);
        }

        @Override
        public void onSubscribe(ScalarSubscription s) {
            this.subscription = s;
        }

        @Override
        public void onComplete(Object value) {
            super.complete();
        }

        @Override
        public void onError(Throwable ex) {
            postGlobalFailure(ex);
        }
    }

    class DaemonEnter implements ScalarSubscriber<Object> {

        @Override
        public void onComplete(Object value) { }

        @Override
        public void onError(Throwable ex) {
            postGlobalFailure(ex);
        }
    }
}
