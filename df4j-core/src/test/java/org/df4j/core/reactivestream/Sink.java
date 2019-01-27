package org.df4j.core.reactivestream;

import org.df4j.core.boundconnector.reactivestream.ReactiveInput;
import org.df4j.core.tasknode.Action;
import org.df4j.core.tasknode.messagescalar.AllOf;
import org.df4j.core.tasknode.messagestream.Actor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * receives totalNumber of Longs and cancels the subscription
 */
class Sink extends Actor implements Subscriber<Long> {
    int maxNumber;
    ReactiveInput<Long> subscriber;
    int received = 0;

    public Sink(AllOf reactiveStreamExample, int maxNumber) {
        reactiveStreamExample.registerAsyncResult(asyncResult());
        if (maxNumber==0) {
            subscriber = new ReactiveInput<Long>(this);
            subscriber.cancel();
            ReactiveStreamMulticastTest.println("  sink: countDown");
            asyncResult().complete();
        } else {
            subscriber = new ReactiveInput<Long>(this);
            this.maxNumber = maxNumber;
            start();
        }
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        subscriber.onSubscribe(subscription);
    }

    @Override
    public void onNext(Long message) {
        subscriber.post(message);
    }

    @Override
    public void onError(Throwable t) {
        subscriber.postFailure(t);
    }

    @Override
    public void onComplete() {
        subscriber.onComplete();
    }

    @Action
    public void act(Long val) {
        //     ReactorTest.println("  Sink.current()="+val);
        if (val != null) {
            ReactiveStreamMulticastTest.println("  sink: received "+val);
            received++;
            if (received < maxNumber) {
                return;
            }
            subscriber.cancel();
        }
        ReactiveStreamMulticastTest.println("  sink: countDown");
        stop();
    }
}
