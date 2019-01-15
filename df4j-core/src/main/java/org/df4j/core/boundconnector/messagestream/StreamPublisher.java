package org.df4j.core.boundconnector.messagestream;

import org.reactivestreams.Subscription;

public interface StreamPublisher<M> {

    Subscription subscribe(StreamSubscriber<M> subscriber);
}
