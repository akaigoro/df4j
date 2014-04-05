package com.github.rfqu.df4j.benchmarkGP;

import org.junit.Test;

import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.core.DFContext;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Creates 10000 chained Actors that are passing messages along. Messages are passed to the first actor and the messages are propagated
 * to the others.
 *
 * test taken from GPars - Groovy Parallel Systems
 * org.codehaus.gpars.javademo.benchmark;
 * @author of original test Vaclav Pech, Lukas Krecan, Pavel Jetensky, Michal Franc
 * @author of the ported test Alexei Kaigorodov
 */
public class ActorBenchmarkTest {
    private static final int ACTORS = 10000;
    private static final int MESSAGES = 500;

    @Test
    public void testBenchmark() throws Exception {

        for (int index = 0; index <= 3; index++) {
            final int concurrencyLevel = 2;//20

            // All actors in this group share the same thread pool
//            final PGroup group = new DefaultPGroup(new DefaultPool(true, concurrencyLevel));
            DFContext.setFixedThreadPool(concurrencyLevel);

            final long t1 = System.currentTimeMillis();
            // With each message received counter is decreased by the actors
            final int latchCount = ACTORS * MESSAGES;
            final CountDownLatch cdl = new CountDownLatch(latchCount);
            Actor<String> lastActor = null;

            // Create and chain actors (backwards - the lastActor will be first)
            for (int i = 0; i < ACTORS; i++) {
                final Actor<String> actor = new MessagePassingActor(lastActor, cdl);
//                actor.setParallelGroup(group);
                lastActor = actor;
//                actor.start();
            }

            for (int i = 0; i < MESSAGES; i++) {
                lastActor.post("Hi");
            }

            cdl.await(1000, TimeUnit.SECONDS);

//            group.shutdown();
            DFContext.getCurrentExecutorService().shutdown();
            final long t2 = System.currentTimeMillis();
            System.out.println("Time to process " + latchCount + " messages: " + (t2 - t1) + " ms");
            assertEquals("Latch has not been decreased to 0", 0, cdl.getCount());
        }

    }

    /**
     * Actor implementation.
     */
    public static class MessagePassingActor extends Actor<String> {

        private final Actor<String> soFarLast;
        private final CountDownLatch cdl;

        MessagePassingActor(final Actor<String> soFarLast, final CountDownLatch cdl) {
            this.soFarLast = soFarLast;
            this.cdl = cdl;
        }

        /**
         * Pass message to next actor and decrease the latch
         *
         * @param msg
         * @return
         */
        @Override
        protected void act(String msg) throws Exception {
            if (soFarLast != null) soFarLast.post(msg);
            cdl.countDown();
        }
    }
}
