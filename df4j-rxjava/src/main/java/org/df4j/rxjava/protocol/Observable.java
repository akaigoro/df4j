package org.df4j.rxjava.protocol;

/**
 * Unlimited message stream without backpressure
 *
 * analogue of {@link org.df4j.protocol.Flood}
 *
 * Consists of:
 * {@link io.reactivex.rxjava3.core.ObservableSource},
 * {@link io.reactivex.rxjava3.core.Observer},
 * {@link io.reactivex.rxjava3.disposables.Disposable}.
 */
public class Observable {
    private Observable(){}
}
