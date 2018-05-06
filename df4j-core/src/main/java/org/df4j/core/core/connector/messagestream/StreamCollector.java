package org.df4j.core.core.connector.messagestream;

import org.df4j.core.core.connector.messagescalar.ScalarCollector;

public interface StreamCollector<T> extends ScalarCollector<T> {

    /** closes the message stream */
    void complete();
}
