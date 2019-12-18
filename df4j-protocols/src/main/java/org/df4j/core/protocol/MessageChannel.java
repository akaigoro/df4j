package org.df4j.core.protocol;

public class MessageChannel {

    private MessageChannel() {}

    /**
     * A {@link Producer} is a provider of a series of tokens, transmitting them to a {@link Consumer}(s).
     * <p>
     * @param <T>  type of tokens
     */
    public interface Producer<T> {
        default Throwable getCompletionException() {
            return null;
        }
        default boolean isCompleted() {
            return false;
        }

        /**
         * {@link Consumer} gets data from {@link Producer}
         * when it has room to save the data.
         * @return the data from  {@link Producer}
         */
        T remove();
    }

    /**
     * A {@link Consumer} can serve multiple {@link Producer}s subscribed dynamically
     * at various points in time.
     *
     * @param <T> the type of element signaled.
     */
    public interface Consumer<T> {

        /**
         * Request from {@link Producer} to transmit scalar data.
         * The data must be ready.
         * <p>
         * {@link Consumer} will take the data immediately or later via {@link Producer#remove()}.
         *
         * @param producer the {@link Producer} which offers the signals for this {@link Consumer}
         */
        void offer(Producer<T> producer);

        /**
         * Cancels the request to transmit data
         *
         * @param producer {@link Producer} which refuses to wait for possibility to transmit data
         */
        void cancel(Producer<? super T> producer);
    }
}
