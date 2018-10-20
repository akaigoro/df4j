package org.df4j.core.boundconnector.messagescalar;

import org.df4j.core.simplenode.messagescalar.SubscriberFuture;

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
     * @param <S> the type of the subscriber
     * @return the subscriber argument
     */
    <S extends ScalarSubscriber<? super T>> S subscribe(S subscriber);

    /**
     *
     * @return Future subscribed to this publisher
     */
    default Future<T> asFuture() {
        return subscribe(new SubscriberFuture<T>());
    }

}
