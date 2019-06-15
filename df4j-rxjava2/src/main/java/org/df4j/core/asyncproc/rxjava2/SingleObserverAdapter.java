package org.df4j.core.asyncproc.rxjava2;

import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import org.df4j.core.asyncproc.ScalarSubscriber;
import org.df4j.core.asyncproc.base.ScalarSubscription;

public class SingleObserverAdapter<T> implements SingleObserver<T> {
    private final ScalarSubscriber<T> scalar;

    public SingleObserverAdapter(ScalarSubscriber<T> scalar) {
        this.scalar = scalar;
    }

    @Override
    public void onSubscribe(Disposable d) {
        ScalarSubscription subscription = new ScalarSubscriptionAdapter(d);
        scalar.onSubscribe(subscription);
    }

    @Override
    public void onSuccess(T o) {
        scalar.onComplete(o);
    }

    @Override
    public void onError(Throwable e) {
        scalar.onError(e);
    }

    private class ScalarSubscriptionAdapter implements ScalarSubscription {
        private final Disposable d;

        public ScalarSubscriptionAdapter(Disposable d) {
            this.d = d;
        }

        @Override
        public void dispose() {
            d.dispose();
        }

        @Override
        public boolean isDisposed() {
            return d.isDisposed();
        }
    }
}
