package org.df4j.core.boundconnector.reactivestream;

import org.df4j.core.boundconnector.messagestream.StreamSubscriber;

/**
 * receiver of message stream with back pressure
 */
public interface ReactiveSubscriber<T> extends StreamSubscriber<T> {

    void onSubscribe(ReactiveSubscription subscription);
}
