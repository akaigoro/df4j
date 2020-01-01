package org.df4j.core.activities;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.portadapter.OutReact;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.io.PrintStream;

/**
 * emits totalNumber of Longs and closes the stream
 */
public class ReactivePublisher extends Actor implements Publisher<Long> {
    private final int delay;
    public OutReact<Long> out = new OutReact<>(this);
    long val;
//    Logger log = new Logger(false);
    Logger log = new Logger(true);

    public ReactivePublisher(long totalNumber, int delay) {
        this.val = totalNumber;
        this.delay = delay;
    }

    public ReactivePublisher(long totalNumber) {
        this(totalNumber,0);
    }

    @Override
    public void subscribe(Subscriber<? super Long> s) {
        out.subscribe(s);
    }

    @Override
    protected void runAction() throws InterruptedException {
        if (val > 0) {
   //         println("out.onNext("+val+")");
            out.onNext(val);
            val--;
            if (delay>0) Thread.sleep(delay);
        } else {
  //          println("out.onComplete() ");
            stop();
            out.onComplete();
        }
    }

    protected void println(String s) {
        s = this.toString()+" "+s;
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
