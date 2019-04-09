package org.df4j.core.actor;

import org.reactivestreams.Subscriber;

public class StreamSubscriptionSource extends Source<Long> {
    Logger log;
    public StreamSubscriptionQueue<Long> output = new StreamSubscriptionQueue<>(this);
    long val = 0;

    public StreamSubscriptionSource(Logger parent, int totalNumber) {
        super(parent);
        log = parent;
        this.val = totalNumber;
    }

    public StreamSubscriptionSource(long totalNumber) {
        this.val = totalNumber;
    }

    @Override
    public void subscribe(Subscriber<? super Long> subscriber) {
        output.subscribe(subscriber);
    }

    @Override
    protected void runAction() {
        StreamSubscription<Long> subscription = output.current();
        if (val > 0) {
            log.println("Source.pub.post("+val+")");
            subscription.onNext(val);
            val--;
        } else {
            log.println("Source.pub.complete()");
            output.onComplete();
            stop();
        }
    }
}
