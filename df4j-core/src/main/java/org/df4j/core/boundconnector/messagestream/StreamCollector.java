package org.df4j.core.boundconnector.messagestream;

import org.df4j.core.boundconnector.messagescalar.ScalarCollector;

public interface StreamCollector<T> extends ScalarCollector<T> {

    void post(T message);

    /** closes the message stream */
    void complete();

    @Override
    default boolean complete(T message) {
        post(message);
        complete();
        return false;
    }
}
