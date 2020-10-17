package org.df4j.rxjava.protocol;

/**
 *  One-shot signal with completion exceptions
 *
 * analogue of {@link org.df4j.protocol.Completable} which actually follows RxJava
 *
 * Consists of:
 * {@link io.reactivex.rxjava3.core.CompletableSource},
 * {@link io.reactivex.rxjava3.core.CompletableObserver},
 * {@link io.reactivex.rxjava3.disposables.Disposable}.
 */
public class Completable {
    private Completable(){}
}
