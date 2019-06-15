package org.df4j.core.asyncproc;

import org.df4j.core.asyncproc.base.ScalarSubscription;
import org.df4j.core.asyncproc.base.ScalarSubscriptionImpl;

import java.util.function.BiConsumer;

public interface ScalarSubscriber<T> extends BiConsumer<T, Throwable> {
    /**
     * Invoked after calling {@link ScalarPublisher#subscribe(ScalarSubscriber)}.
     *
     * @param s
     *            {@link ScalarSubscriptionImpl} that allows cancelling subscription via {@link ScalarSubscriptionImpl#cancel()} }
     */
    default void onSubscribe(ScalarSubscription s) {}

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
}
