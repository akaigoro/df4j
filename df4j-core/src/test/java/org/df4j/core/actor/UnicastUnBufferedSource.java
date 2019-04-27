package org.df4j.core.actor;

import org.df4j.core.SubscriptionCancelledException;
import org.reactivestreams.Subscriber;

/**
 * emits totalNumber of Longs and closes the stream
 */
public class UnicastUnBufferedSource extends Source<Long> {
    public StreamSubscriptionConnector<Long> output = new StreamSubscriptionConnector<>(this);
    long val = 0;

    public UnicastUnBufferedSource(Logger parent, long totalNumber) {
        super(parent);
        this.val = totalNumber;
    }

    public UnicastUnBufferedSource(long totalNumber) {
        this.val = totalNumber;
    }

    @Override
    public void subscribe(Subscriber<? super Long> subscriber) {
        output.subscribe(subscriber);
    }

    @Override
    protected void runAction() {
        if (val > 0) {
            println("UnicastUnBufferedSource:subscription.onNext("+val+")");
            try {
                output.onNext(val);
                val--;
            } catch (SubscriptionCancelledException e) {
            }
        } else {
            println("UnicastUnBufferedSource:subscription.onComplete()");
            output.onComplete();
            stop();
        }
    }
}
