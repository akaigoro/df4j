package org.df4j.core.actor.permitstream;

import org.df4j.core.actor.StreamOutput;
import org.df4j.core.actor.Hactor;
import org.df4j.core.asyncproc.AllOf;
import org.df4j.core.actor.Semafor;
import org.df4j.core.actor.Actor;
import org.df4j.core.actor.StreamProcessor;
import org.df4j.core.asyncproc.ScalarResult;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *  This is a demonstration how backpressure can be implemented using plain {@link Semafor}
 */
public class SignalStreamExample {

    /** making a feedback loop: permits flow from {@link Sink} to {@link Source#backPressureActuator}.
     */
    @Test
    public void minimalTest() throws InterruptedException, TimeoutException, ExecutionException {
        int totalCount = 3;
        Source first = new Source(totalCount);
        Sink last = new Sink(first.backPressureActuator);
        first.pub.subscribe(last);

        AllOf all = new AllOf();
        all.registerAsyncResult(first, last);

        first.start();
        ScalarResult<Void> asyncResult = all.asyncResult();
  //      Thread.sleep(400);
        asyncResult.get(400000, TimeUnit.MILLISECONDS);
        assertEquals(totalCount, last.totalCount);
    }

    /** making a feedback loop: permits flow from {@link Sink} to {@link Source#backPressureActuator}.
     */
    @Test
    public void piplineTest() throws InterruptedException, TimeoutException, ExecutionException {
        int totalCount = 3;
        Source first = new Source(totalCount);
        TestProcessor testProcessor = new TestProcessor();
        TestProcessor testProcessor1 = new TestProcessor();
        Sink last = new Sink(first.backPressureActuator);

        first.pub.subscribe(testProcessor);
        testProcessor.subscribe(testProcessor1);
        testProcessor1.subscribe(last);
        first.start();

        AllOf all = new AllOf();
        all.registerAsyncResult(first, testProcessor, testProcessor1, last);
        last.asyncResult().get(400, TimeUnit.MILLISECONDS);
        assertEquals(totalCount, last.totalCount);
    }

    public static class Source extends Actor {
        Semafor backPressureActuator = new Semafor(this);
        StreamOutput<Integer> pub = new StreamOutput<>(this);
        int count;

        Source(int count) {
            this.count = count;
        }

        protected void runAction() {
            if (count > 0) {
                pub.onNext(count);
                count--;
            } else {
                pub.onComplete();
                stop();
            }
        }
    }

    static class TestProcessor extends StreamProcessor<Integer, Integer> {
        {
            start();
        }

        @Override
        protected Integer process(Integer message) {
            return message;
        }
    }

    // Hactor is stopped when mainInput is completed
    static class Sink extends Hactor<Integer> {
        final Semafor backPressureActuator;
        int totalCount = 0;

        Sink(Semafor backPressureActuator) {
            this.backPressureActuator = backPressureActuator;
            backPressureActuator.release();
            start();
        }

        protected void runAction(Integer message) throws Exception {
            totalCount++;
            backPressureActuator.release();
        }
    }
}
