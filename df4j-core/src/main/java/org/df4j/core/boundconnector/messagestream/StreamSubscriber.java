package org.df4j.core.boundconnector.messagestream;

import org.df4j.core.boundconnector.messagescalar.SimpleSubscription;

public interface StreamSubscriber<T> extends StreamCollector<T> {

    void onSubscribe(SimpleSubscription subscription);

}
