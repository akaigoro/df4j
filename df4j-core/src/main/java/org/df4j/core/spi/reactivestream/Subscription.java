package org.df4j.core.spi.reactivestream;

/**
 * Message control linking a {@link Publisher} and {@link
 * Subscriber}.  Subscribers receive items only when requested,
 * and may cancel at any time. The methods in this interface are
 * intended to be invoked only by their Subscribers; usages in
 * other contexts have undefined effects.
 */
public interface Subscription {
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

    /**
     * Causes the Subscriber to (eventually) stop receiving
     * messages.  Implementation is best-effort -- additional
     * messages may be received after invoking this method.
     * A cancelled subscription need not ever receive an
     * {@code onComplete} or {@code completeExceptionally} signal.
     */
    public void cancel();
}
