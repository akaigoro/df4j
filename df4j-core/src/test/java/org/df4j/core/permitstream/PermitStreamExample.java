package org.df4j.core.permitstream;

import org.df4j.core.connector.messagestream.StreamOutput;
import org.df4j.core.connector.permitstream.OneShotPermitPublisher;
import org.df4j.core.connector.permitstream.Semafor;
import org.df4j.core.node.Actor;
import org.df4j.core.node.Actor1;
import org.df4j.core.node.messagestream.StreamProcessor;
import org.df4j.core.util.SameThreadExecutor;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

/**
 *  This is a demonstration how backpressure can be implemented using {@link Semafor}
 */
public class PermitStreamExample {
    @Test
    public void piplineTest() throws InterruptedException {
        int totalCount = 10;
        Source first = new Source(totalCount);
        Sink last = new Sink();
        first.pub.subscribe(new TestProcessor())
                .subscribe(new TestProcessor())
                .subscribe(last)
                .backPressureCommander.subscribe(first.backPressureActuator);
        first.start();
        last.fin.await();
        assertEquals(totalCount, last.totalCount);
    }

    static class Source extends Actor {
        Semafor backPressureActuator = new Semafor(this);
        StreamOutput<Integer> pub = new StreamOutput<>(this);
        int count;

        Source(int count) {
            this.count = count;
            setExecutor(new SameThreadExecutor());
        }

        @Override
        protected void act() throws Exception {
            if (count == 0) {
                pub.complete();
            } else {
                pub.post(count);
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
        OneShotPermitPublisher backPressureCommander = new OneShotPermitPublisher();
        int totalCount = 0;
        CountDownLatch fin = new CountDownLatch(1);

        {
            backPressureCommander.release(1);
            start();
        }

        @Override
        protected void act(Integer message) throws Exception {
            totalCount++;
            backPressureCommander.release(1);
        }

        @Override
        protected void onCompleted() {
            fin.countDown();
        }
    }
}
