package org.df4j.core.reactivestream;

import org.df4j.core.boundconnector.reactivestream.ReactiveUnicastOutput;
import org.df4j.core.tasknode.messagescalar.AllOf;
import org.reactivestreams.Subscriber;

import java.io.PrintStream;

/**
 * emits totalNumber of Longs and closes the stream
 */
public class UnicastSource extends Source<Long> {
    ReactiveUnicastOutput<Long> pub = new ReactiveUnicastOutput<>(this);
    long val = 0;

    public UnicastSource(AllOf parent, int totalNumber) {
        super(parent);
        this.val = totalNumber;
    }

    public UnicastSource(long totalNumber) {
        this.val = totalNumber;
    }

    public UnicastSource() {
    }

    @Override
    public void subscribe(Subscriber<? super Long> subscriber) {
        pub.subscribe(subscriber);
    }

    @Override
    protected void runAction() {
        if (val > 0) {
            println("Source.pub.post("+val+")");
            pub.post(val);
            val--;
        } else {
            pub.complete();
            println("Source.pub.complete()");
            stop();
        }
    }

    static PrintStream out = System.out;
    static void println(String s) {
        out.println(s);
        out.flush();
    }
}
