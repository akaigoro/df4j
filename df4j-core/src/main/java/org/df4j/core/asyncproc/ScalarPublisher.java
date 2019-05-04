package org.df4j.core.asyncproc;

import io.reactivex.SingleObserver;
import io.reactivex.SingleSource;
import io.reactivex.disposables.Disposable;
import org.df4j.core.asyncproc.base.ScalarSubscriptionImpl;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.CompletableFuture;

/**
 * A {@link ScalarPublisher} is a provider of a single element, publishing it to a {@link ScalarSubscriber}(s).
 * <p>
 * A {@link ScalarPublisher} can serve multiple {@link ScalarSubscriber}s subscribed {@link #subscribe(ScalarSubscriber)} dynamically
 * at various points in time.
 *
 * @param <R> the type of element signaled.
 */
public interface ScalarPublisher<R> extends SingleSource<R> {

    /**
     * Request {@link ScalarPublisher} to start scalar data.
     * <p>
     * This is a "factory method" and can be called multiple times, each time starting a new {@link ScalarSubscriptionImpl}.
     * <p>
     * Each {@link ScalarSubscriptionImpl} will work for only a single {@link ScalarSubscriber}.
     * <p>
     * A {@link ScalarSubscriber} should only subscribe once to a single {@link ScalarPublisher}.
     * <p>
     * If the {@link ScalarPublisher} rejects the subscription attempt or otherwise fails it will
     * signal the error via {@link ScalarSubscriber#onError}.
     *
     * @param s the {@link ScalarSubscriber} that will consume signals from this {@link ScalarPublisher}
     */
    void subscribe(ScalarSubscriber<? super R> s);

    @Override
    default void subscribe(SingleObserver<? super R> observer) {
        if (observer == null) {
            throw new NullPointerException();
        }
        ScalarSubscriber<? super R> s = new ScalarSubscriber<R>() {
            @Override
            public void onSubscribe(Disposable s) {
                observer.onSubscribe(s);
            }

            @Override
            public void onComplete(R r) {
                observer.onSuccess(r);
            }

            @Override
            public void onError(Throwable t) {
                observer.onError(t);
            }
        };
        subscribe(s);
    }

    default void subscribe(CompletableFuture<? super R> cf) {
        if (cf == null) {
            throw new NullPointerException();
        }
        ScalarSubscriber<? super R> proxySubscriber = new ScalarSubscriber<R>() {

            @Override
            public void onSubscribe(Disposable s) {}

            @Override
            public void onComplete(R t) {
                cf.complete(t);
            }

            @Override
            public void onError(Throwable t) {
                cf.completeExceptionally(t);
            }
        };
        subscribe(proxySubscriber);
    }

}
