package org.df4j.core.protocols;

public class PermitStream {

    private PermitStream() {}

    /**
     * A {@link Publisher} is a provider of a potentially unbounded number of permits
     */
    @FunctionalInterface
    public interface Publisher {

        /**
         * was: onSendTo
         *
         * @param subscriber
         *      the {@link Subscriber} that will consume signals from this {@link Publisher}
         * @return Subscription which can be cancelled, or null if subscribtion object is not created
         */
        Disposable subscribe(Subscriber subscriber);
    }

    /**
     *  inlet for permits.
     *
     */
    public interface Subscriber extends Runnable {
        /**
         * Adds the given number {@code n} of items to the current
         * unfulfilled demand for this subscription.  If {@code n} is
         * less than or equal to zero, the Subscriber will receive an
         * {@code postFailure} signal with an {@link
         * IllegalArgumentException} argument.  Otherwise, the
         * Subscriber will receive up to {@code n} additional {@code
         * post} invocations (or fewer if terminated).
         *
         * @param n the increment of demand; a value of {@code
         * Long.MAX_VALUE} may be considered as effectively unbounded
         */
        void release(long n);

        default void release() {
            release(1);
        }

        default void run() {
            release(1);
        }
    }
}
