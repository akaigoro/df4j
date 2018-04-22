package org.df4j.core.spi.reactivestream;

/**
 * A producer of items (and related control messages) received by
 * Subscribers.  Each current {@link Subscriber} receives the same
 * items (via method {@code complete}) in the same order, unless
 * drops or errors are encountered. If a Publisher encounters an
 * error that does not allow items to be issued to a Subscriber,
 * that Subscriber receives {@code completeExceptionally}, and then receives no
 * further messages.  Otherwise, when it is known that no further
 * messages will be issued to it, a subscriber receives {@code
 * onComplete}.  Publishers ensure that Subscriber method
 * invocations for each subscription are strictly ordered in <a
 * href="package-summary.html#MemoryVisibility"><i>happens-before</i></a>
 * order.
 *
 * <p>Publishers may vary in policy about whether drops (failures
 * to issue an item because of resource limitations) are treated
 * as unrecoverable errors.  Publishers may also vary about
 * whether Subscribers receive items that were produced or
 * available before they subscribed.
 *
 * @param <T> the published item type
 */
@FunctionalInterface
public interface Publisher<T> {
    /**
     * Adds the given Subscriber if possible.  If already
     * subscribed, or the attempt to subscribe fails due to policy
     * violations or errors, the Subscriber's {@code completeExceptionally}
     * method is invoked with an {@link IllegalStateException}.
     * Otherwise, the Subscriber's {@code onSubscribe} method is
     * invoked with a new {@link Subscription}.  Subscribers may
     * enable receiving items by invoking the {@code request}
     * method of this Subscription, and may unsubscribe by
     * invoking its {@code cancel} method.
     *
     * @param subscriber the subscriber
     * @throws NullPointerException if subscriber is null
     */
    public void subscribe(Subscriber<? super T> subscriber);
}
