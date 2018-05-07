package org.df4j.core.connector.messagestream;

import org.df4j.core.connector.messagescalar.SimpleSubscription;

public interface StreamSubscriber<T> extends StreamCollector<T> {

    void onSubscribe(SimpleSubscription subscription);

}
