package org.df4j.core;

import org.df4j.core.asyncproc.ScalarSubscription;

public interface ScalarSubscriber<T> {
    /**
     * Invoked after calling {@link ScalarPublisher#subscribe(ScalarSubscriber)}.
     * <p>
     * No data will start flowing until {@link ScalarSubscription#request(long)} is invoked.
     * <p>
     * It is the responsibility of this {@link ScalarSubscriber} instance to call {@link ScalarSubscription#request(long)} whenever more data is wanted.
     * <p>
     * The {@link ScalarPublisher} will send notifications only in response to {@link ScalarSubscription#request(long)}.
     *
     * @param s
     *            {@link ScalarSubscription} that allows requesting data via {@link ScalarSubscription#request(long)}
     */
    default void onSubscribe(ScalarSubscription s) {}

    /**
     * Data notification sent by the {@link ScalarPublisher} in response to requests to {@link ScalarSubscription#request(long)}.
     *
     * @param t the element signaled
     */
     void onComplete(T t);

    /**
     * Failed terminal state.
     * <p>
     * No further events will be sent even if {@link ScalarSubscription#request(long)} is invoked again.
     *
     * @param t the throwable signaled
     */
    default void onError(Throwable t) {}
}
