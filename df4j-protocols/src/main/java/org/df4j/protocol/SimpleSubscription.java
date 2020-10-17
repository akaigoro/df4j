package org.df4j.protocol;

import org.reactivestreams.Publisher;

/**
 * for on-shot subscriptions, where subscriber is unsubscribed by publisher after single message transmission.
 */
public interface SimpleSubscription {

    /**
     * Request the {@link Publisher} to stop sending data and clean up resources.
     * <p>
     * Data may still be sent to meet previously signalled demand after calling cancel.
     */
    public void cancel();
}
