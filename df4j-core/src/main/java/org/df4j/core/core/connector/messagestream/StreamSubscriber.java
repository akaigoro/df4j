package org.df4j.core.core.connector.messagestream;

import org.df4j.core.core.connector.messagescalar.SimpleSubscription;

public interface StreamSubscriber<T> extends StreamCollector<T> {

    void onSubscribe(SimpleSubscription subscription);

}
