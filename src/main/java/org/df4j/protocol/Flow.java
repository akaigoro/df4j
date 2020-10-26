package org.df4j.protocol;

/************************************************************************
 * Licensed under Public Domain (CC0)                                    *
 *                                                                       *
 * To the extent possible under law, the person who associated CC0 with  *
 * this code has waived all copyright and related or neighboring         *
 * rights to this code.                                                  *
 *                                                                       *
 * You should have received a copy of the CC0 legalcode along with this  *
 * work. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.*
 ************************************************************************/

/**
 * Flow of messages with back-pressure
 * <p>
 *  This class declaration is for reference only.
 *  Actual declarations used are in package {@link org.reactivestreams}
 * @see <a href="https://www.reactive-streams.org/">https://www.reactive-streams.org/</a>
 */
public final class Flow {

    private Flow() {} // uninstantiable

    public interface Publisher<T> extends org.reactivestreams.Publisher<T>, Scalar.Publisher<T> {
        @Override
        default void subscribe(Scalar.Subscriber<? super T> observer) {
            Media<T> media = new Media<T>(observer);
            subscribe(media);
        }

        class Media<T> implements org.reactivestreams.Subscriber<T>, SimpleSubscription {
            private final Scalar.Subscriber<? super T> observer;
            private org.reactivestreams.Subscription subscription;
            private boolean cancelled;

            public Media(Scalar.Subscriber<? super T> observer) {
                this.observer = observer;
            }

            @Override
            public boolean isCancelled() {
                return cancelled;
            }

            @Override
            public void onSubscribe(org.reactivestreams.Subscription subscription) {
                this.subscription = subscription;
                observer.onSubscribe(this);
                subscription.request(1);
            }

            @Override
            public void onNext(T t) {
                observer.onSuccess(t);
                subscription.cancel();
            }

            @Override
            public void onError(Throwable t) {
                observer.onError(t);
            }

            @Override
            public void onComplete() {
                observer.onSuccess(null);
            }

            @Override
            public synchronized void cancel() {
                cancelled = true;
            }
        }
    }

    public interface Subscriber<T> extends org.reactivestreams.Subscriber<T> {

    }

}
