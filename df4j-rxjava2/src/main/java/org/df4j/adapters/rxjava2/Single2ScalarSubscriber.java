package org.df4j.adapters.rxjava2;

import io.reactivex.SingleObserver;
import org.df4j.core.protocol.ScalarMessage;

public class Single2ScalarSubscriber<R> implements ScalarMessage.Subscriber<R>, io.reactivex.disposables.Disposable {
    private final SingleObserver<? super R> observer;
    private org.df4j.core.protocol.Disposable subscriptopn;

    public Single2ScalarSubscriber(SingleObserver<? super R> observer) {
        this.observer = observer;
    }

    @Override
    public void onSubscribe(org.df4j.core.protocol.Disposable subscriptopn) {
        this.subscriptopn = subscriptopn;
        observer.onSubscribe((io.reactivex.disposables.Disposable)this);
    }

    @Override
    public void onSuccess(R r) {
        observer.onSuccess(r);
    }

    @Override
    public void onError(Throwable t) {
        observer.onError(t);
    }


    @Override
    public void dispose() {
        subscriptopn.dispose();
    }

    @Override
    public boolean isDisposed() {
        return subscriptopn.isDisposed();
    }
}
