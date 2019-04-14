package org.df4j.core.asyncproc;

public interface ScalarSubscriber<T> {
    /**
     * Invoked after calling {@link ScalarPublisher#subscribe(ScalarSubscriber)}.
     * <p>
     * No data will start flowing until {@link ScalarSubscriptionQueue.ScalarSubscription#request(long)} is invoked.
     * <p>
     * It is the responsibility of this {@link ScalarSubscriber} instance to call {@link ScalarSubscriptionQueue.ScalarSubscription#request(long)} whenever more data is wanted.
     * <p>
     * The {@link ScalarPublisher} will send notifications only in response to {@link ScalarSubscriptionQueue.ScalarSubscription#request(long)}.
     *
     * @param s
     *            {@link ScalarSubscriptionQueue.ScalarSubscription} that allows requesting data via {@link ScalarSubscriptionQueue.ScalarSubscription#request(long)}
     */
    public void onSubscribe(ScalarSubscriptionQueue.ScalarSubscription s);

    /**
     * Data notification sent by the {@link ScalarPublisher} in response to requests to {@link ScalarSubscriptionQueue.ScalarSubscription#request(long)}.
     *
     * @param t the element signaled
     */
    public void onComplete(T t);

    /**
     * Failed terminal state.
     * <p>
     * No further events will be sent even if {@link ScalarSubscriptionQueue.ScalarSubscription#request(long)} is invoked again.
     *
     * @param t the throwable signaled
     */
    public void onError(Throwable t);
}
