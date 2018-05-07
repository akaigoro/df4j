package org.df4j.core.connector.messagestream;

import org.df4j.core.connector.messagescalar.ScalarCollector;

public interface StreamCollector<T> extends ScalarCollector<T> {

    /** closes the message stream */
    void complete();
}
