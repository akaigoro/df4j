package org.df4j.core.boundconnector.messagestream;

import org.df4j.core.boundconnector.messagescalar.ScalarCollector;

public interface StreamCollector<T> extends ScalarCollector<T> {

    /**
     * If this ScalarSubscriber was not already completed, sets it completed state.
     * @param message
     * @return true if this message caused this ScalarSubscriber instance to asyncTask to a completed state, else false
     */
    void post(T message);

    /**
     * If this ScalarSubscriber was not already completed, sets it completed state.
     * @param ex
     * @return true if this exception caused this ScalarSubscriber instance to asyncTask to a completed state, else false
     */
    default boolean completeExceptionally(Throwable ex) {return false;}

    /** closes the message stream */
    void complete();

    @Override
    default boolean complete(T message) {
        post(message);
        complete();
        return false;
    }
}
