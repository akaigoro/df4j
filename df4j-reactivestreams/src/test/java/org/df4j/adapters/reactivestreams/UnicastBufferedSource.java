package org.df4j.adapters.reactivestreams;

import org.df4j.core.actor.StreamOutput;
import org.df4j.core.asyncproc.AllOf;

import java.io.PrintStream;
import java.util.concurrent.Flow;

import static org.df4j.core.util.Utils.sneakyThrow;

/**
 * emits totalNumber of Longs and closes the stream
 */
public class UnicastBufferedSource extends org.df4j.core.actor.Actor implements Flow.Publisher<Long> {
    public StreamOutput<Long> output = new StreamOutput<>(this);
    long val;
    Logger log;

    public UnicastBufferedSource(Logger parent, long totalNumber) {
        UnicastBufferedSource.this.log = parent;
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

    protected void println(String s) {
        if (log != null) {
            log.println(s);
        } else {
            System.out.println(s); // TODO remove
        }
    }

    @Override
    public synchronized void stopExceptionally(Throwable t) {
        super.stopExceptionally(t);
        sneakyThrow(t);
    }

    public static class Logger extends AllOf {
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
