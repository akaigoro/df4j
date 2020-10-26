package org.df4j.rxjava.port;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.MaybeObserver;
import io.reactivex.rxjava3.core.MaybeSource;
import org.df4j.core.connector.ScalarResult;
import org.df4j.protocol.Scalar;
import org.df4j.protocol.SimpleSubscription;

public class OutMaybe<T> extends ScalarResult implements MaybeSource<T> {

    @Override
    public void subscribe(@NonNull MaybeObserver<? super T> observer) {
        Scalar.Subscriber proxySubscriber = new ProxySubscriber(observer);
        super.subscribe(proxySubscriber);
    }

    private class ProxySubscriber implements Scalar.Subscriber<T>, io.reactivex.rxjava3.disposables.Disposable {
        private  MaybeObserver<? super T> observer;
        private SimpleSubscription scalarSubscription;

        public ProxySubscriber(MaybeObserver<? super T> observer) {
            this.observer = observer;
        }

        @Override
        public void onSubscribe(SimpleSubscription subscription) {
            scalarSubscription = subscription;
            observer.onSubscribe(this);
        }

        @Override
        public void onSuccess(T t) {
            observer.onSuccess(t);
        }

        @Override
        public void onError(Throwable t) {
            observer.onError(t);
        }

        public void onComplete() {
            observer.onComplete();
        }

        @Override
        public void dispose() {
            scalarSubscription.cancel();
        }

        @Override
        public boolean isDisposed() {
            return scalarSubscription.isCancelled();
        }
    }
}
