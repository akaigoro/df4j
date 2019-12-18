package org.df4j.core.fancy;

import org.df4j.core.actor.AsyncProc;
import org.df4j.core.protocol.Disposable;
import org.df4j.core.protocol.Scalar;

public class AllOf extends AsyncSupplier<Void> {

    public AllOf() {
    }

    public AllOf(Scalar.Publisher<?>... sources) {
        for (Scalar.Publisher source: sources) {
            registerAsyncResult(source);
        }
    }

    /**
     * blocks this instance from completion until the source is completed.
     *
     * @param source source of completion. successfull or unseccessfull
     */
    public synchronized void registerAsyncResult(Scalar.Publisher source) {
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
    public synchronized void registerAsyncDaemon(Scalar.Publisher source) {
        source.subscribe(new DaemonEnter());
    }

    @Override
    protected void fire() {
        completeResult(null);
    }

    protected synchronized void postGlobalFailure(Throwable ex) {
        completeResultExceptionally(ex);
    }

    class Enter extends Port implements Scalar.Subscriber<Object> {
        Disposable subscription;

        public Enter() {
            super(AllOf.this);
        }

        @Override
        public void onSubscribe(Disposable s) {
            this.subscription = s;
        }

        @Override
        public void onSuccess(Object value) {
            super.complete();
        }

        @Override
        public void onError(Throwable ex) {
            postGlobalFailure(ex);
        }
    }

    class DaemonEnter implements Scalar.Subscriber<Object> {

        @Override
        public void onSuccess(Object value) { }

        @Override
        public void onError(Throwable ex) {
            postGlobalFailure(ex);
        }
    }
}
