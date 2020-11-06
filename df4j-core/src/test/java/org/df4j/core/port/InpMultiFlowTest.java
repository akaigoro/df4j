package org.df4j.core.port;

import org.df4j.core.activities.PublisherActor;
import org.df4j.core.actor.Actor;
import org.df4j.core.actor.ActorGroup;
import org.df4j.core.util.Utils;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CompletionException;

public class InpMultiFlowTest {

    static class TestActor<T> extends Actor {
        InpMultiFlow<T> inp;
        InpSignal signal = new InpSignal(this);
        T value;
        int runCounter = 0;

        public TestActor(ActorGroup graph, int capacity) {
            super(graph);
            inp = new InpMultiFlow<>(this, capacity);
        }

        @Override
        protected void runAction() {
            value = inp.remove();
            runCounter++;
            signal.acquire();
        }
    }

    @Test
    public void simpleTest() throws CompletionException, InterruptedException {
        int cnt = 3;
        TestActor<Integer> actor = new TestActor<>(new ActorGroup(), 2);
        InpMultiFlow<Integer> inp = actor.inp;
        actor.setExecutor(Utils.directExec);
        Assert.assertFalse(inp.isReady());
        inp.add(1);
        Assert.assertEquals(0, actor.runCounter);
        Assert.assertTrue(inp.isReady());
        inp.add(2);
        try {
            inp.add(3);
            Assert.fail("buffer overflow expected");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalStateException);
        }
        actor.signal.release();
        actor.start();
        Thread.sleep(100);
        Assert.assertEquals(1, actor.runCounter);
        Assert.assertEquals(1, actor.value.intValue());
        actor.signal.release();
        Assert.assertEquals(2, actor.runCounter);
        Assert.assertEquals(2, actor.value.intValue());
        inp.add(3);
        Assert.assertTrue(inp.isReady());
        actor.signal.release();
        Thread.sleep(100);
        Assert.assertFalse(inp.isReady());
        try {
            inp.remove();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalStateException);
        }
    }

    public void testInpMultiFlow(int cnt, int delay1, int delay2) throws InterruptedException {
        ActorGroup graph = new ActorGroup();
        PublisherActor producer = new PublisherActor(graph, cnt, delay1);
        PublisherActor publisher = new PublisherActor(graph, cnt, delay2);
        TestActor<Long> subscriber = new TestActor<>(graph, 3);
        subscriber.inp.subscribeTo(producer);
        subscriber.inp.subscribeTo(publisher);
        producer.start();
        publisher.start();
        subscriber.start();
        subscriber.signal.release(cnt*2);
        boolean fin = graph.await(1000);
        Assert.assertTrue(fin);
    }

    @Test
    public void testAsyncQueueProdSubComplete() throws InterruptedException {
        testInpMultiFlow(0,0, 0);
    }

    @Test
    public void testAsyncQueueSlowProd() throws InterruptedException {
        testInpMultiFlow(5,100, 0);
    }

    @Test
    public void testAsyncQueueSlowConsComplete() throws InterruptedException {
        testInpMultiFlow(0,0, 100);
    }

    @Test
    public void testAsyncQueueSlowCons() throws InterruptedException {
        testInpMultiFlow(3,0, 100);
    }
}

