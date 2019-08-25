package org.df4j.core.actor;


import java.util.concurrent.Flow;

/**
 * emits totalNumber of Longs and closes the stream
 */
public class UnicastBufferedSource extends Source<Long> {
    public StreamOutput<Long> output = new StreamOutput<>(this);
    long val;

    public UnicastBufferedSource(Logger parent, long totalNumber) {
        super(parent);
        this.val = totalNumber;
        if (totalNumber == 0) {
            output.onComplete();
            stop();
        }
    }

    @Override
    public void subscribe(Flow.Subscriber<? super Long> subscriber) {
        output.subscribe(subscriber);
    }

    @Override
    protected void runAction() {
        if (val > 0) {
            println("UnicastBufferedSource:subscription.onNext("+val+")");
            output.onNext(val);
            val--;
        } else {
            println("UnicastBufferedSource:subscription.onComplete()");
            output.onComplete();
            stop();
        }
    }
}
