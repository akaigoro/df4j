package org.df4j.core.simplenode.messagescalar;

import org.df4j.core.boundconnector.Port;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.df4j.core.boundconnector.messagescalar.ScalarPublisher;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 *
 * @param <R> type of input parameter
 * @param <R> type of result
 */
public class CompletablePromise<R> extends CompletableFuture<R> implements Port<R>, ScalarPublisher<R> {

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
    public Subscription subscribe(Subscriber<R> subscriber) {
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

    public void onComplete() {
        super.complete(null);
    }

    class ScalarSubscription implements Subscription, BiConsumer<R, Throwable> {

        private final Subscriber<? super R> subscriber;
        private final CompletableFuture<R> cp;

        public <S extends Subscriber<? super R>> ScalarSubscription(S subscriber) {
            this.subscriber = subscriber;
            cp = CompletablePromise.this.whenComplete(this);
            subscriber.onSubscribe(this);
        }

        @Override
        public void accept(R r, Throwable throwable) {
            if (throwable != null) {
                subscriber.onError(throwable);
            } else {
                subscriber.onNext(r);
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
