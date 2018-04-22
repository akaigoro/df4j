package org.df4j.core.spi.permitstream;

import org.df4j.core.spi.reactivestream.Subscription;

/**
 *  inlet for permits.
 *
 *  method descriptions are taken from description of class {@link Subscription}.
 */
public interface PermitConsumer {
    /**
     * Adds the given number {@code n} of items to the current
     * unfulfilled demand for this subscription.  If {@code n} is
     * less than or equal to zero, the Subscriber will receive an
     * {@code completeExceptionally} signal with an {@link
     * IllegalArgumentException} argument.  Otherwise, the
     * Subscriber will receive up to {@code n} additional {@code
     * complete} invocations (or fewer if terminated).
     *
     * @param n the increment of demand; a value of {@code
     * Long.MAX_VALUE} may be considered as effectively unbounded
     */
    public void request(long n);
}
