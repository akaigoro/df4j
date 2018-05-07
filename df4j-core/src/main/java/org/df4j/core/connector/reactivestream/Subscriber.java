package org.df4j.core.connector.reactivestream;

import org.df4j.core.connector.messagestream.StreamCollector;

/**
 * receiver of message stream with back pressure
 */
public interface Subscriber<T> extends StreamCollector<T> {

    void onSubscribe(Subscription subscription);
}
