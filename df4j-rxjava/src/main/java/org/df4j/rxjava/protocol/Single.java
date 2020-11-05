package org.df4j.rxjava.protocol;

import org.df4j.protocol.Scalar;

/**
 *  One-shot message with completion exceptions
 *
 * analogue of {@link Scalar}
 *
 * Consists of:
 * {@link io.reactivex.rxjava3.core.SingleSource},
 * {@link io.reactivex.rxjava3.core.SingleObserver},
 * {@link io.reactivex.rxjava3.disposables.Disposable}.
 */
public class Single {
    private Single(){}
}
