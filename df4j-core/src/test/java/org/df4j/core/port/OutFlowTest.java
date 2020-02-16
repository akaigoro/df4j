package org.df4j.core.port;

import org.df4j.core.activities.LoggingSubscriber;
import org.df4j.core.activities.PublisherActor;
import org.df4j.core.dataflow.Actor;
import org.df4j.core.util.CurrentThreadExecutor;
import org.df4j.core.util.Utils;
import org.junit.Assert;
import org.junit.Test;

public class OutFlowTest {
    static class TestActor extends Actor {
        int runCounter = 0;
        @Override
        protected void runAction() throws Throwable {
            runCounter++;
            suspend();
        }
    }

    @Test
    public void simpleTest() throws InterruptedException {
        int cnt = 3;
        TestActor actor = new TestActor();
        OutFlow<Integer> out = new OutFlow<>(actor, 2);
        actor.setExecutor(Utils.directExec);
        actor.start();
        Assert.assertTrue(out.isReady());
        out.onNext(1);
        Assert.assertEquals(actor.runCounter, 1);
        Assert.assertTrue(out.isReady());
        out.onNext(2);
        try {
            out.onNext(3);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalStateException);
        }
        Assert.assertEquals(actor.runCounter, 1);
        actor.resume();
        Assert.assertEquals(actor.runCounter, 1);
        Assert.assertEquals(1, out.poll().intValue());
        Assert.assertTrue(out.isReady());
        Assert.assertEquals(actor.runCounter, 2);
        Assert.assertEquals(2, out.poll().intValue());
        Assert.assertTrue(out.isReady());
        Assert.assertNull(out.poll());
        Assert.assertTrue(out.isReady());
    }

    @Test
    public void outFlowTest() throws InterruptedException {
        CurrentThreadExecutor executor = new CurrentThreadExecutor();
        int cnt = 3;
        PublisherActor pub = new PublisherActor(cnt, 0);
        pub.setExecutor(executor);
        LoggingSubscriber sub = new LoggingSubscriber();
        pub.out.subscribe(sub);
        pub.start();
        executor.executeAll();
        boolean success = pub.blockingAwait(0);
        Assert.assertTrue(success);
        Thread.sleep(50);
        Assert.assertEquals(cnt, sub.cnt);
        Assert.assertTrue(sub.completed);
    }
}

