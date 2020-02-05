package org.df4j.rxjava.port;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.MaybeObserver;
import org.df4j.core.dataflow.AsyncProc;
import org.df4j.core.port.InpScalar;
import org.df4j.protocol.SimpleSubscription;

import java.util.concurrent.locks.ReentrantLock;

public class InpMaybe<T> extends InpScalar<T> implements MaybeObserver<T> {
    /**
     * @param parent {@link AsyncProc} to which this port belongs
     */
    public InpMaybe(AsyncProc parent) {
        super(parent);
    }

    @Override
    public void onSubscribe(@NonNull io.reactivex.rxjava3.disposables.Disposable d) {
        SimpleSubscription proxySub = new ProxySub(d);
        super.onSubscribe(proxySub);
    }

    public void onComplete() {
        onError(null);
    }

    static class ProxySub implements SimpleSubscription {
        private io.reactivex.rxjava3.disposables.Disposable d;
        private ReentrantLock sLock = new ReentrantLock();

        public ProxySub(@NonNull io.reactivex.rxjava3.disposables.Disposable d) {
            this.d = d;
        }

        @Override
        public void cancel() {
            sLock.lock();
            io.reactivex.rxjava3.disposables.Disposable dd;
            try {
                dd = d;
                if (dd == null) {
                    return;
                }
                d = null;
            } finally {
                sLock.unlock();
            }
            dd.dispose();
        }

        @Override
        public boolean isCancelled() {
            sLock.lock();
            io.reactivex.rxjava3.disposables.Disposable dd;
            try {
                return d == null;
            } finally {
                sLock.unlock();
            }
        }
    }
}
