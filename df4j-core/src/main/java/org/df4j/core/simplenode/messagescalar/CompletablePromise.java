package org.df4j.core.simplenode.messagescalar;

import org.df4j.core.boundconnector.messagescalar.ScalarPublisher;
import org.df4j.core.boundconnector.messagescalar.ScalarSubscriber;
import org.df4j.core.boundconnector.SimpleSubscription;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 *
 * @param <R> type of input parameter
 * @param <R> type of result
 */
public class CompletablePromise<R> extends CompletableFuture<R> implements ScalarSubscriber<R>, ScalarPublisher<R> {

    @Override
    public void post(R message) {
        super.complete(message);
    }

    @Override
    public void postFailure(Throwable ex) {
        super.completeExceptionally(ex);
    }
    @Override
    public CompletablePromise<R> asFuture() {
        return this;
    }

    @Override
    public SimpleSubscription subscribe(ScalarSubscriber<R> subscriber) {
        ScalarSubscription<R> subscription = new ScalarSubscription<>(subscriber);
        super.whenComplete(subscription);
        return subscription;
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

    public void complete() {
        super.complete(null);
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
                subscriber.postFailure(throwable);
            } else {
                subscriber.post(r);
            }
        }
    }
}
