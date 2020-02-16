package org.df4j.core.asyncarrayblockingqueue;

import org.df4j.core.activities.LoggingSubscriber;
import org.df4j.core.activities.ThreadPublisher;
import org.df4j.core.dataflow.Dataflow;
import org.junit.Assert;
import org.junit.Test;

import java.util.logging.Level;

public class TreadPubTest {

    public void pubSubTest(int cnt, int delay1) throws InterruptedException {
        ThreadPublisher pub = new ThreadPublisher(cnt, delay1);
        LoggingSubscriber sub = new LoggingSubscriber();
        sub.logger.setLevel(Level.ALL);
        pub.subscribe(sub);
        pub.start();
        pub.join(400);
        Assert.assertEquals(cnt, sub.cnt);
        Thread.sleep(400);
        Assert.assertTrue(sub.completed);
        Assert.assertNull(sub.completionException);
    }

    @Test
    public void pubSubTest0() throws InterruptedException {
        pubSubTest(0, 0);
    }

    @Test
    public void pubSubTest1() throws InterruptedException {
        pubSubTest(1, 0);
    }

    @Test
    public void pubSubTest4() throws InterruptedException {
        pubSubTest(4, 0);
    }

    @Test
    public void slowPubTest() throws InterruptedException {
        pubSubTest(3, 100);
    }
}

