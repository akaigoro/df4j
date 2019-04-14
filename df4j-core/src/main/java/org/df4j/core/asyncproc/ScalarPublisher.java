package org.df4j.core.asyncproc;

/**
 * A {@link ScalarPublisher} is a provider of a single element, publishing it to a {@link ScalarSubscriber}(s).
 * <p>
 * A {@link ScalarPublisher} can serve multiple {@link ScalarSubscriber}s subscribed {@link #subscribe(ScalarSubscriber)} dynamically
 * at various points in time.
 *
 * @param <R> the type of element signaled.
 */
public interface ScalarPublisher<R> {

    /**
     * Request {@link ScalarPublisher} to start scalar data.
     * <p>
     * This is a "factory method" and can be called multiple times, each time starting a new {@link ScalarSubscriptionQueue.ScalarSubscription}.
     * <p>
     * Each {@link ScalarSubscriptionQueue.ScalarSubscription} will work for only a single {@link ScalarSubscriber}.
     * <p>
     * A {@link ScalarSubscriber} should only subscribe once to a single {@link ScalarPublisher}.
     * <p>
     * If the {@link ScalarPublisher} rejects the subscription attempt or otherwise fails it will
     * signal the error via {@link ScalarSubscriber#onError}.
     *
     * @param s the {@link ScalarSubscriber} that will consume signals from this {@link ScalarPublisher}
     */
    public void subscribe(ScalarSubscriber<? super R> s);
}
