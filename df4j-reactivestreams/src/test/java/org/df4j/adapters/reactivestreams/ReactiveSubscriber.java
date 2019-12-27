package org.df4j.adapters.reactivestreams;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.Dataflow;
import org.junit.Assert;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.PrintStream;

/**
 * emits totalNumber of Longs and closes the stream
 */
public class ReactiveSubscriber extends Actor implements Subscriber<Long> {
    public InpReactiveStream<Long> input = new InpReactiveStream<>(this);
    long expectedVal;
    Logger log = new Logger(false);

    public ReactiveSubscriber(Dataflow parent, long totalNumber) {
        super(parent);
        this.expectedVal = totalNumber;
    }

    @Override
    protected void runAction() {
        if (input.isCompleted()) {
            println("ReactiveSubscriber:input.isCompleted()");
            stop();
        } else {
            long nextVal = input.remove();
            println("ReactiveSubscriber.input.onNext("+nextVal+")");
            Assert.assertEquals(expectedVal, nextVal);
            expectedVal--;
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
    public void onSubscribe(Subscription s) {
        input.onSubscribe(s);
    }

    @Override
    public void onNext(Long aLong) {
        input.onNext(aLong);
    }

    @Override
    public void onError(Throwable t) {
        input.onError(t);
    }

    @Override
    public void onComplete() {
        input.onComplete();
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
