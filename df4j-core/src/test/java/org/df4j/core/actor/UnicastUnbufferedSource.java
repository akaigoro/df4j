package org.df4j.core.actor;

import org.reactivestreams.Subscriber;

/**
 * emits totalNumber of Longs and closes the stream
 */
public class UnicastUnbufferedSource extends Source<Long> {
    public StreamSubscriptionBlockingQueue<Long> output = new StreamSubscriptionBlockingQueue<>(this);
    long val = 0;

    public UnicastUnbufferedSource(Logger parent, int totalNumber) {
        super(parent);
        this.val = totalNumber;
    }

    public UnicastUnbufferedSource(long totalNumber) {
        this.val = totalNumber;
    }

    @Override
    public void subscribe(Subscriber<? super Long> subscriber) {
        output.subscribe(subscriber);
    }

    @Override
    protected void runAction() {
        if (val > 0) {
            StreamSubscription<Long> subscription = output.current();
            if (subscription == null) {
                return;
            }
            println("UnicastUnbufferedSource:subscription.onNext("+val+")");
            subscription.onNext(val);
            val--;
        } else {
            println("UnicastUnbufferedSource:subscription.onComplete()");
            output.onComplete();
            stop();
        }
    }
}
