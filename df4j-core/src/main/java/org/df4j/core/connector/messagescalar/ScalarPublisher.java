package org.df4j.core.connector.messagescalar;

import java.util.concurrent.Future;

/**
 * this is a Promise interface
 * @param <T> the published item type
 */
@FunctionalInterface
public interface ScalarPublisher<T> {
    /**
     * Adds the given Subscriber if possible.  If already
     * subscribed, or the attempt to subscribe fails due to policy
     * violations or errors, the Subscriber's {@code postFailure}
     * method is invoked with an {@link IllegalStateException}.
     *
     * @param subscriber the subscriber
     * @throws NullPointerException if subscriber is null
     */
    <S extends ScalarSubscriber<? super T>> S subscribe(S subscriber);

    default Future<T> asFuture() {
        return subscribe(new SubscriberFuture<T>());
    }

}
