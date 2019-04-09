package org.df4j.core.actor;

import org.reactivestreams.Subscriber;

/**
 * emits totalNumber of Longs and closes the stream
 */
public class UnicastSource extends Source<Long> {
    Logger log;
    public StreamSubscriptionQueue<Long> output = new StreamSubscriptionQueue<>(this);
    long val = 0;

    public UnicastSource(Logger parent, int totalNumber) {
        super(parent);
        log = parent;
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
            StreamSubscription<Long> subscription = output.current();
            if (val > 0) {
                println("UnicastSource:subscription.onNext("+val+")");
                subscription.onNext(val);
                val--;
            } else {
                println("UnicastSource:subscription.onComplete()");
                subscription.onComplete();
                stop();
            }
        } catch (Throwable t) {
            println("UnicastSource: catch"+t);
            t.printStackTrace();
        }
    }

    private void println(String s) {
        if (log != null) {
            log.println(s);
        } else {
            System.out.println(s); // TODO remove
        }
    }
}
