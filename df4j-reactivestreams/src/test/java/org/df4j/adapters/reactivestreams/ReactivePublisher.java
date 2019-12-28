package org.df4j.adapters.reactivestreams;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.portadapter.OutReact;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.io.PrintStream;

/**
 * emits totalNumber of Longs and closes the stream
 */
public class ReactivePublisher extends Actor implements Publisher<Long> {
    public OutReact<Long> output = new OutReact<>(this);
    long val;
    Logger log = new Logger(false);

    public ReactivePublisher(Dataflow parent, long totalNumber) {
        super(parent);
        this.val = totalNumber;
        if (totalNumber == 0) {
            output.onComplete();
            stop();
        }
    }

    public ReactivePublisher(long totalNumber) {
        this(new Dataflow(), totalNumber);
    }

    @Override
    public void subscribe(Subscriber<? super Long> s) {
        output.subscribe(s);
    }

    @Override
    protected void runAction() {
        if (val > 0) {
            println("ReactivePublisher:output.onNext("+val+")");
            output.onNext(val);
            val--;
        } else {
            println("ReactivePublisher:output.onComplete()");
            output.onComplete();
            stop();
        }
    }

    protected void println(String s) {
        if (log != null) {
            log.println(s);
        } else {
            System.out.println(s); // TODO remove
        }
    }

    public static class Logger {
        PrintStream out = System.out;
        final boolean printOn;

        public Logger(boolean printOn) {
            this.printOn = printOn;
        }

        void println(String s) {
            if (!printOn) {
                return;
            }
            out.println(s);
            out.flush();
        }
    }
}
