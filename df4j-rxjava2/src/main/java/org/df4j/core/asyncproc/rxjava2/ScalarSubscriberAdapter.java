package org.df4j.core.asyncproc.rxjava2;

import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import org.df4j.core.asyncproc.ScalarSubscriber;
import org.df4j.core.asyncproc.base.ScalarSubscription;

public class ScalarSubscriberAdapter<R> implements ScalarSubscriber<R> {
    private final SingleObserver<? super R> observer;

    public ScalarSubscriberAdapter(SingleObserver<? super R> observer) {
        this.observer = observer;
    }

    @Override
    public void onSubscribe(ScalarSubscription s) {
        Disposable subscription = new DisposableAdapter(s);
        observer.onSubscribe(subscription);
    }

    @Override
    public void onComplete(R r) {
        observer.onSuccess(r);
    }

    @Override
    public void onError(Throwable t) {
        observer.onError(t);
    }

    private class DisposableAdapter implements Disposable {
        private final ScalarSubscription s;

        public DisposableAdapter(ScalarSubscription s) {
            this.s = s;
        }

        @Override
        public void dispose() {
            s.dispose();
        }

        @Override
        public boolean isDisposed() {
            return s.isDisposed();
        }
    }
}
