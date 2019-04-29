package org.df4j.core.asyncproc;

public interface ScalarSubscriber<T> {
    /**
     * Invoked after calling {@link ScalarPublisher#subscribe(ScalarSubscriber)}.
     *
     * @param s
     *            {@link ScalarSubscription} that allows cancelling subscription via {@link ScalarSubscription#cancel()} }
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
}
