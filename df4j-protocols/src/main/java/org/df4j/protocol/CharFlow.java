package org.df4j.protocol;

import org.reactivestreams.Subscription;

public class CharFlow {
    private CharFlow(){}

    public interface CharPublisher {
        void subscribe(CharSubscriber subscriber);
    }

    public interface CharSubscriber {
        void onSubscribe(Subscription subscription);

        void onNext(char c);

        void onComplete();

        void onError(Throwable e);
    }
}
