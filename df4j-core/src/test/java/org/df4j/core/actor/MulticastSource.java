package org.df4j.core.actor;

import org.reactivestreams.Subscriber;

/**
 * emits totalNumber of Integers and closes the stream
 */
public class MulticastSource extends Source<Long> {
    protected MulticastStreamOutput<Long> pub = new MulticastStreamOutput<>(this);
    long val = 0;

    public MulticastSource() {
    }

    public MulticastSource(long totalNumber) {
        this.val = totalNumber;
    }

    public MulticastSource(Logger parent, long totalNumber) {
        super(parent);
        this.val = totalNumber;
    }

    @Override
    public void subscribe(Subscriber<? super Long> subscriber) {
        pub.subscribe(subscriber);
    }

    @Override
    protected void runAction() {
        if (val == 0) {
            println("MulticastSource.pub.complete()");
            pub.onComplete();
            stop();
        } else {
            println("MulticastSource.pub.post "+ val);
            pub.onNext(val);
            val--;
        }
    }
}
