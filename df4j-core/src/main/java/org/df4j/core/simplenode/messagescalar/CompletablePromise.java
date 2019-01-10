package org.df4j.core.simplenode.messagescalar;

import org.df4j.core.boundconnector.messagescalar.ScalarPublisher;
import org.df4j.core.boundconnector.messagescalar.ScalarSubscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 *
 * @param <R> type of input parameter
 * @param <R> type of result
 */
public class CompletablePromise<R> extends CompletableFuture<R> implements ScalarSubscriber<R>, ScalarPublisher<R> {

    @Override
    public void onNext(R message) {
        super.complete(message);
    }

    @Override
    public void onError(Throwable ex) {
        super.completeExceptionally(ex);
    }
    @Override
    public CompletablePromise<R> asFuture() {
        return this;
    }

    @Override
    public Subscription subscribe(ScalarSubscriber<R> subscriber) {
        ScalarSubscription<R> subscription = new ScalarSubscription<>(subscriber);
        super.whenComplete(subscription);
        subscriber.onSubscribe(subscription);
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

    static class ScalarSubscription<R> implements Subscription, BiConsumer<R, Throwable> {

        private final ScalarSubscriber<? super R> subscriber;

        public <S extends ScalarSubscriber<? super R>> ScalarSubscription(S subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void request(long n) {
        }

        @Override
        public void cancel() {
        }

        @Override
        public void accept(R r, Throwable throwable) {
            if (throwable != null) {
                subscriber.onError(throwable);
            } else {
                subscriber.onNext(r);
            }
        }
    }
}
