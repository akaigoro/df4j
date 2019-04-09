package org.df4j.core.actor;

import org.reactivestreams.Subscriber;

/**
 * emits totalNumber of Longs and closes the stream
 */
public class UnicastUnbuffereSource extends Source<Long> {
    public StreamSubscriptionBlockingQueue<Long> output = new StreamSubscriptionBlockingQueue<>(this);
    long val = 0;

    public UnicastUnbuffereSource(Logger parent, int totalNumber) {
        super(parent);
        this.val = totalNumber;
    }

    public UnicastUnbuffereSource(long totalNumber) {
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
