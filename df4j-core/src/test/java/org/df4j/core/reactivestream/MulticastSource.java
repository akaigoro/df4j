package org.df4j.core.reactivestream;

import org.df4j.core.boundconnector.reactivestream.ReactiveMulticastOutput;
import org.df4j.core.tasknode.Action;
import org.df4j.core.tasknode.messagescalar.AllOf;
import org.reactivestreams.Subscriber;

/**
 * emits totalNumber of Integers and closes the stream
 */
class MulticastSource extends Source<Long> {
    ReactiveMulticastOutput<Long> pub = new ReactiveMulticastOutput<>(this);
    long val = 0;

    public MulticastSource() {
    }

    public MulticastSource(long totalNumber) {
        this.val = totalNumber;
    }

    public MulticastSource(AllOf parent, long totalNumber) {
        super(parent);
        this.val = totalNumber;
    }

    @Override
    public void subscribe(Subscriber<? super Long> subscriber) {
        pub.subscribe(subscriber);
    }

    @Action
    public void act() {
        if (val == 0) {
            pub.onComplete();
            ReactiveStreamMulticastTest.println("MulticastSource.pub.post()");
            stop();
        } else {
            //          ReactorTest.println("pub.post("+val+")");
            pub.post(val);
            val--;
        }
    }
}
