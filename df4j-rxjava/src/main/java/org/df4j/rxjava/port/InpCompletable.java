package org.df4j.rxjava.port;

import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import org.df4j.core.dataflow.AsyncProc;
import org.df4j.core.port.CompletablePort;

/**
 * One-shot token storage for a black Completion token (signal+error).
 * After the token is received, this port stays ready forever.
 */
public class InpCompletable extends CompletablePort implements CompletableObserver {
    private Disposable subscription;

    /**
     * @param parent {@link AsyncProc} to which this port belongs
     */
    public InpCompletable(AsyncProc parent) {
        super(parent, false);
    }

    @Override
    public void onSubscribe(Disposable subscription) {
        this.subscription = subscription;
    }
}
