package org.df4j.core.permitstream;

import org.df4j.core.boundconnector.messagestream.StreamOutput;
import org.df4j.core.boundconnector.permitstream.Semafor;
import org.df4j.core.tasknode.Action;
import org.df4j.core.tasknode.messagestream.Actor;
import org.df4j.core.tasknode.messagestream.Actor1;
import org.df4j.core.tasknode.messagestream.StreamProcessor;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 *  This is a demonstration how backpressure can be implemented using plain {@link Semafor}
 */
public class PermitStreamExample {

    /** making a feedback loop: permits flow from {@link Sink} to {@link Source#backPressureActuator}.
     */
    @Test
    public void piplineTest() throws InterruptedException {
        int totalCount = 10;
        Source first = new Source(totalCount);
        TestProcessor testProcessor = new TestProcessor();
        TestProcessor testProcessor1 = new TestProcessor();
        Sink last = new Sink(first.backPressureActuator);

        first.pub.subscribe(testProcessor);
        testProcessor.subscribe(testProcessor1);
        testProcessor1.subscribe(last);
        first.start();
        last.fin.await(2, TimeUnit.SECONDS);
        assertEquals(totalCount, last.totalCount);
    }

    public static class Source extends Actor {
        Semafor backPressureActuator = new Semafor(this);
        StreamOutput<Integer> pub = new StreamOutput<>(this);
        int count;

        Source(int count) {
            this.count = count;
        }

        @Action
        public void act() {
            if (count == 0) {
                pub.onComplete();
            } else {
                pub.onNext(count);
                count--;
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

    static class Sink extends Actor1<Integer> {
        final Semafor backPressureActuator;
        int totalCount = 0;
        CountDownLatch fin = new CountDownLatch(1);

        Sink(Semafor backPressureActuator) {
            this.backPressureActuator = backPressureActuator;
            backPressureActuator.release();
            start();
        }

        protected void runAction(Integer message) throws Exception {
            totalCount++;
            backPressureActuator.release();
        }

        @Override
        protected void completion() throws Exception {
            fin.countDown();
        }
    }
}
