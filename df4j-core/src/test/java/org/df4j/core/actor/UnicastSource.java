package org.df4j.core.actor;

import org.reactivestreams.Subscriber;

/**
 * emits totalNumber of Longs and closes the stream
 */
public class UnicastSource extends Source<Long> {
    public StreamSubscriptionQueue<Long> output = new StreamSubscriptionQueue<>(this);
    long val = 0;

    public UnicastSource(Logger parent, int totalNumber) {
        super(parent);
        this.val = totalNumber;
    }

    public UnicastSource(long totalNumber) {
        this.val = totalNumber;
    }

    @Override
    public void subscribe(Subscriber<? super Long> subscriber) {
        output.subscribe(subscriber);
    }

    @Override
    protected void runAction() {
        try {
            if (val > 0) {
                StreamSubscription<Long> subscription = output.current();
                if (subscription == null) {
                    return;
                }
                println("UnicastSource:subscription.onNext("+val+")");
                subscription.onNext(val);
                val--;
            } else {
                println("UnicastSource:subscription.onComplete()");
                output.onComplete();
                stop();
            }
        } catch (Throwable t) {
            println("UnicastSource: catch"+t);
            t.printStackTrace();
        }
    }
}
