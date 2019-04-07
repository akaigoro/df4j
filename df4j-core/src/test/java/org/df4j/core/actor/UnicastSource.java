package org.df4j.core.actor;

import org.reactivestreams.Subscriber;

/**
 * emits totalNumber of Longs and closes the stream
 */
public class UnicastSource extends Source<Long> {
    Logger log;
    public StreamOutput<Long> output = new StreamOutput<>(this);
    long val = 0;

    public UnicastSource(Logger parent, int totalNumber) {
        super(parent);
        log = parent;
        this.val = totalNumber;
    }

    public UnicastSource(long totalNumber) {
        this.val = totalNumber;
    }

    public UnicastSource() {
    }

    @Override
    public void subscribe(Subscriber<? super Long> subscriber) {
        output.subscribe(subscriber);
    }

    @Override
    protected void runAction() {
        if (val > 0) {
            log.println("Source.pub.post("+val+")");
            output.onNext(val);
            val--;
        } else {
            log.println("Source.pub.complete()");
            output.onComplete();
            stop();
        }
    }
}
