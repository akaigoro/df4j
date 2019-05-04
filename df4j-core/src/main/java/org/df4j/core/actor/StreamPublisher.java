package org.df4j.core.actor;

import org.df4j.core.asyncproc.ScalarPublisher;
import org.df4j.core.asyncproc.ScalarSubscriber;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public interface StreamPublisher<T> extends Publisher<T>, ScalarPublisher<T> {

    default void subscribe (ScalarSubscriber< ? super T > s){
        Scalar2StreamSubscriber proxySubscriber = new Scalar2StreamSubscriber(s);
        subscribe(proxySubscriber);
    }

    class Scalar2StreamSubscriber<T> implements Subscriber<T> {
        private ScalarSubscriber scalarSubscriber;
        private Subscription subscription;

        public Scalar2StreamSubscriber(ScalarSubscriber<? super T> s) {
            scalarSubscriber = s;
        }

        @Override
        public void onSubscribe(Subscription s) {
            subscription = s;
            s.request(1);
        }

        @Override
        public void onNext(T t) {
            scalarSubscriber.onComplete(t);
            subscription.cancel();
        }

        @Override
        public void onError(Throwable t) {
            scalarSubscriber.onError(t);
            subscription.cancel();
        }

        @Override
        public void onComplete() {
            scalarSubscriber.onComplete(null);
            subscription.cancel();
        }
    }
}
