package org.df4j.protocol;

import org.reactivestreams.Subscription;

public class CharFlow {
    private CharFlow(){}

    public interface Publisher {
        void subscribe(Subscriber subscriber);
    }

    public interface Subscriber {
        void onSubscribe(Subscription subscription);

        void onNext(char c);

        void onComplete();

        void onError(Throwable e);
    }
}
