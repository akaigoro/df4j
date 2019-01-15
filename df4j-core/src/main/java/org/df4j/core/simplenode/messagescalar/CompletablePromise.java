package org.df4j.core.simplenode.messagescalar;

import org.reactivestreams.Subscription;
import org.df4j.core.boundconnector.messagescalar.ScalarPublisher;
import org.df4j.core.boundconnector.messagescalar.ScalarSubscriber;

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
    public Subscription subscribe(ScalarSubscriber<R> subscriber) {
        ScalarSubscription subscription = new ScalarSubscription(subscriber);
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

    class ScalarSubscription implements Subscription, BiConsumer<R, Throwable> {

        private final ScalarSubscriber<? super R> subscriber;
        private final CompletableFuture<R> cp;

        public <S extends ScalarSubscriber<? super R>> ScalarSubscription(S subscriber) {
            this.subscriber = subscriber;
            cp = CompletablePromise.this.whenComplete(this);
            subscriber.onSubscribe(this);
        }

        @Override
        public void accept(R r, Throwable throwable) {
            if (throwable != null) {
                subscriber.postFailure(throwable);
            } else {
                subscriber.post(r);
            }
        }

        @Override
        public void request(long n) {}

        @Override
        public void cancel() {
            cp.cancel(true);
        }
    }
}
