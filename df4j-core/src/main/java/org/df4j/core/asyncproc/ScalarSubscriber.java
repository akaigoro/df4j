package org.df4j.core.asyncproc;

import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import org.df4j.core.asyncproc.base.ScalarSubscriptionImpl;

import java.util.function.BiConsumer;

public interface ScalarSubscriber<T> extends BiConsumer<T, Throwable> {
    /**
     * Invoked after calling {@link ScalarPublisher#subscribe(ScalarSubscriber)}.
     *
     * @param s
     *            {@link ScalarSubscriptionImpl} that allows cancelling subscription via {@link ScalarSubscriptionImpl#cancel()} }
     */
    default void onSubscribe(Disposable s) {}

    /**
     * Data notification sent by the {@link ScalarPublisher}
     *
     * @param t the element signaled
     */
     void onComplete(T t);

    /**
     * Failed terminal state.
     * <p>
     * No further events will be sent.
     *
     * @param t the throwable signaled
     */
    default void onError(Throwable t) {}

    @Override
    default void accept(T t, Throwable throwable) {
        if (throwable == null) {
            onComplete(t);
        } else {
            onError(throwable);
        }
    }

    /**
     * enables to subscribe to rxjava2 {@link io.reactivex.SingleSource}
     * @return
     */
    default SingleObserver<T> asSingleObserver() {
        return new SingleObserver<T>() {

            @Override
            public void onSubscribe(Disposable d) {
                ScalarSubscriber.this.onSubscribe(d);
            }

            @Override
            public void onSuccess(T o) {
                onComplete(o);
            }

            @Override
            public void onError(Throwable e) {
                ScalarSubscriber.this.onError(e);
            }
        };
    }
}
