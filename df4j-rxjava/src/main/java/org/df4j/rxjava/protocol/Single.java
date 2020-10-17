package org.df4j.rxjava.protocol;

/**
 *  One-shot message with completion exceptions
 *
 * analogue of {@link org.df4j.protocol.Scalar}
 *
 * Consists of:
 * {@link io.reactivex.rxjava3.core.SingleSource},
 * {@link io.reactivex.rxjava3.core.SingleObserver},
 * {@link io.reactivex.rxjava3.disposables.Disposable}.
 */
public class Single {
    private Single(){}
}
