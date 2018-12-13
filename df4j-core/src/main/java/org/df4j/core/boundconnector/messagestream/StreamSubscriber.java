package org.df4j.core.boundconnector.messagestream;

import org.df4j.core.boundconnector.messagescalar.ScalarSubscriber;

public interface StreamSubscriber<T> extends ScalarSubscriber<T> {

    /** closes the message stream */
    void complete();
}
