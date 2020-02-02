package org.df4j.protocol;


import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * A {@link FlowSubscription} represents a one-to-one lifecycle of a {@link Subscriber} subscribing to a {@link Publisher}.
 * <p>
 * It can only be used once by a single {@link Subscriber}.
 * <p>
 * It is used to both signal desire for data and cancel demand (and allow resource cleanup).
 *
 */
public interface FlowSubscription extends org.reactivestreams.Subscription, SimpleSubscription {
}
