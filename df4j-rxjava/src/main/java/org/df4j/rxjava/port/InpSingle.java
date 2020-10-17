package org.df4j.rxjava.port;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.SingleObserver;
import org.df4j.core.dataflow.AsyncProc;
import org.df4j.core.port.InpScalar;
import org.df4j.protocol.Flow;
import org.df4j.protocol.Scalar;
import org.df4j.protocol.SimpleSubscription;

/**
 * Token storage with standard Subscriber&lt;T&gt; interface.
 * It has place for only one message.
 * After the message is received, this port stays ready forever.
 *
 * It can connect both to {@link Scalar.Source} and {@link Flow.Publisher}.
 *
 * @param <T> type of accepted tokens.
 */
public class InpSingle<T> extends InpScalar<T> implements SingleObserver<T> {

    /**
     * @param parent {@link AsyncProc} to which this port belongs
     */
    public InpSingle(AsyncProc parent) {
        super(parent);
    }

    @Override
    public void onSubscribe(@NonNull io.reactivex.rxjava3.disposables.Disposable d) {
        SimpleSubscription proxySub = new InpMaybe.ProxySub(d);
        super.onSubscribe(proxySub);
    }
}
