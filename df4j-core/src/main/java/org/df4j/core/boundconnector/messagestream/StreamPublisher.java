package org.df4j.core.boundconnector.messagestream;

import org.df4j.core.boundconnector.SimpleSubscription;

public interface StreamPublisher<M> {

    SimpleSubscription subscribe(StreamSubscriber<M> subscriber);
}
