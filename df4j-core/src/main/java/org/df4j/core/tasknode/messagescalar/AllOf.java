package org.df4j.core.tasknode.messagescalar;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

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

    class Enter extends Lock implements Subscriber<Object> {

        @Override
        public void onSubscribe(Subscription s) {

        }

        @Override
        public void onNext(Object value) {
            super.turnOn();
        }

        @Override
        public void onError(Throwable ex) {
            postGlobalFailure(ex);
        }

        @Override
        public void onComplete() {

        }
    }

    class DaemonEnter implements Subscriber<Object> {

        @Override
        public void onSubscribe(Subscription s) {

        }

        @Override
        public void onNext(Object value) { }

        @Override
        public void onError(Throwable ex) {
            postGlobalFailure(ex);
        }

        @Override
        public void onComplete() {

        }
    }
}
