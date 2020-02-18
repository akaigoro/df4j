package org.df4j.rxjava.port;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.MaybeObserver;
import org.df4j.core.dataflow.AsyncProc;
import org.df4j.core.port.InpScalar;
import org.df4j.protocol.SimpleSubscription;

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

        public ProxySub(@NonNull io.reactivex.rxjava3.disposables.Disposable d) {
            this.d = d;
        }

        @Override
        public void cancel() {
            io.reactivex.rxjava3.disposables.Disposable dd;
            synchronized(this) {
                dd = d;
                if (dd == null) {
                    return;
                }
                d = null;
            }
            dd.dispose();
        }

        @Override
        public boolean isCancelled() {
            synchronized(this) {
                return d == null;
            }
        }
    }
}
