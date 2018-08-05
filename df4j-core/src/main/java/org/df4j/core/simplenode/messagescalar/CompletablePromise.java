package org.df4j.core.simplenode.messagescalar;

import org.df4j.core.boundconnector.messagescalar.ScalarPublisher;
import org.df4j.core.boundconnector.messagescalar.ScalarSubscriber;
import org.df4j.core.boundconnector.messagescalar.SimpleSubscription;
import org.df4j.core.tasknode.AsyncProc;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 *
 * @param <R> type of input parameter
 * @param <R> type of result
 */
public class CompletablePromise<R> extends CompletableFuture<R> implements ScalarPublisher<R> {

    protected final AsyncProc asyncProc;

    public CompletablePromise(AsyncProc asyncProc) {
        this.asyncProc = asyncProc;
    }

    public CompletablePromise() {
        this.asyncProc = null;
    }

    @Override
    public CompletablePromise<R> asFuture() {
        return this;
    }

    @Override
    public synchronized <S extends ScalarSubscriber<? super R>> S subscribe(S subscriber) {
        ScalarSubscription<R> subscription = new ScalarSubscription<>(subscriber);
        subscriber.onSubscribe(subscription);
        super.whenComplete(subscription);
        return subscriber;
    }

    /**
     * Returns a new CompletableFuture that is already completed with
     * the given value.
     *
     * @param value the value
     * @param <U> the type of the value
     * @return the completed CompletableFuture
     */
    public static <U> CompletablePromise<U> completedPromise(U value) {
        CompletablePromise<U> result = new CompletablePromise<>();
        result.complete(value);
        return result;
    }

    static class ScalarSubscription<R> implements SimpleSubscription, BiConsumer<R, Throwable> {

        private final ScalarSubscriber<? super R> subscriber;

        public <S extends ScalarSubscriber<? super R>> ScalarSubscription(S subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public boolean cancel() {
            return false;
        }

        @Override
        public void accept(R r, Throwable throwable) {
            if (throwable != null) {
                subscriber.completeExceptionally(throwable);
            } else {
                subscriber.complete(r);
            }
        }
    }
}
