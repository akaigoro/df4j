package org.df4j.core.tasknode.messagescalar;

import org.df4j.core.boundconnector.messagescalar.ScalarPublisher;
import org.df4j.core.boundconnector.messagescalar.ScalarSubscriber;
import org.df4j.core.tasknode.AsyncAction;
import org.df4j.core.tasknode.AsyncProc;

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

    class Enter extends Lock implements ScalarSubscriber<Object> {

        @Override
        public void post(Object value) {
            super.turnOn();
        }

        @Override
        public void postFailure(Throwable ex) {
            postGlobalFailure(ex);
        }
    }

    class DaemonEnter implements ScalarSubscriber<Object> {

        @Override
        public void post(Object value) { }

        @Override
        public void postFailure(Throwable ex) {
            postGlobalFailure(ex);
        }
    }
}
