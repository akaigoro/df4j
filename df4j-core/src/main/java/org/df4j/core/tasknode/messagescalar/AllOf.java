package org.df4j.core.tasknode.messagescalar;

import org.df4j.core.boundconnector.Port;
import org.reactivestreams.Publisher;
import org.df4j.core.tasknode.AsyncAction;

public class AllOf extends AsyncSupplier<Void> {

    public AllOf() {
    }

    public AllOf(Publisher<?>... sources) {
        for (Publisher source: sources) {
            registerAsyncResult(source);
        }
    }

    /**
     * blocks this instance from completion until the source is completed.
     *
     * @param source source of completion. successfull or unseccessfull
     */
    public synchronized void registerAsyncResult(Publisher source) {
        source.subscribe(new Enter());
    }

    public synchronized void registerAsyncResult(AsyncAction... sources) {
        for (AsyncAction source: sources) {
            registerAsyncResult(source.asyncResult());
        }
    }

    /**
     * does not blocks this instance from completion.
     * Used to collect possible exceptions only
     *
     * @param source source of errors
     */
    public synchronized void registerAsyncDaemon(Publisher source) {
        source.subscribe(new DaemonEnter());
    }

    @Override
    protected void fire() {
        completeResult(null);
    }

    protected synchronized void postGlobalFailure(Throwable ex) {
        completeResultExceptionally(ex);
    }

    class Enter extends Lock implements Port<Object> {

        @Override
        public void onNext(Object value) {
            super.turnOn();
        }

        @Override
        public void onError(Throwable ex) {
            postGlobalFailure(ex);
        }
    }

    class DaemonEnter implements Port<Object> {

        @Override
        public void onNext(Object value) { }

        @Override
        public void onError(Throwable ex) {
            postGlobalFailure(ex);
        }
    }
}
