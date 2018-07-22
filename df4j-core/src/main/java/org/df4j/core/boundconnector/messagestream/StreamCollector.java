package org.df4j.core.boundconnector.messagestream;

import org.df4j.core.boundconnector.messagescalar.ScalarCollector;

public interface StreamCollector<T> extends ScalarCollector<T> {

    /** closes the message stream */
    void complete();
}
