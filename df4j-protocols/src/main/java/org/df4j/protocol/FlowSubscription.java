package org.df4j.protocol;

/**
 * A {@link FlowSubscription} represents a one-to-one lifecycle of a {@link Flow.Subscriber} subscribing to a {@link Flow.Publisher}.
 * <p>
 * It can only be used once by a single {@link Flow.Subscriber}.
 * <p>
 * It is used to both signal desire for data and cancel demand (and allow resource cleanup).
 *
 */
public interface FlowSubscription extends ScalarSubscription {
    /**
     * No events will be sent by a {@link Flow.Publisher} until demand is signaled via this method.
     * <p>
     * It can be called however often and whenever needed—but the outstanding cumulative demand must never exceed Long.MAX_VALUE.
     * An outstanding cumulative demand of Long.MAX_VALUE may be treated by the {@link Flow.Publisher} as "effectively unbounded".
     * <p>
     * Whatever has been requested can be sent by the {@link Flow.Publisher} so only signal demand for what can be safely handled.
     * <p>
     * A {@link Flow.Publisher} can send less than is requested if the stream ends but
     * then must emit either {@link Flow.Subscriber#onError(Throwable)} or {@link Flow.Subscriber#onComplete()}.
     *
     * @param n the strictly positive number of elements to requests to the upstream {@link Flow.Publisher}
     */
    public void request(long n);
}
