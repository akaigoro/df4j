package org.df4j.core.asyncproc.base;

import org.df4j.core.asyncproc.ScalarSubscriber;
import org.reactivestreams.Subscriber;

public class Stream2ScalarSubscriber<T> implements ScalarSubscriber<T> {
    private final Subscriber<? super T> streamSubscriber;
    private boolean requested = false;
    private boolean completed = false;
    private T completionValue;

    public Stream2ScalarSubscriber(Subscriber<? super T> streamSubscriber) {
        this.streamSubscriber = streamSubscriber;
    }

    public void request() {
        requested = true;
        if (!completed) {
            return;
        }
        streamSubscriber.onNext(completionValue);
        streamSubscriber.onComplete();
    }

    @Override
    public void onSubscribe(ScalarSubscription subscription) {
        streamSubscriber.onSubscribe((ScalarSubscription.Scalar2StreamSubscription) subscription);
    }

    @Override
    public void onComplete(T t) {
        if (requested) {
            streamSubscriber.onNext(t);
            streamSubscriber.onComplete();
        } else {
            completionValue = t;
            completed = true;
        }
    }

    @Override
    public void onError(Throwable t) {
        streamSubscriber.onError(t);
    }
}
