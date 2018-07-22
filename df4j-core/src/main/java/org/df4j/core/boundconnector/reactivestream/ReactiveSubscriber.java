package org.df4j.core.boundconnector.reactivestream;

import org.df4j.core.boundconnector.messagestream.StreamCollector;

/**
 * receiver of message stream with back pressure
 */
public interface ReactiveSubscriber<T> extends StreamCollector<T> {

    void onSubscribe(ReactiveSubscription subscription);
}
