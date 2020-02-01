package org.df4j.core.dataflow;

import org.df4j.core.activities.PublisherActor;
import org.df4j.core.activities.ScalarSubscriberActor;
import org.df4j.core.activities.SubscriberActor;
import org.df4j.core.util.Utils;
import org.junit.Assert;
import org.junit.Test;

public class PubScalarSubTest {

    public void pubSubTest(int cnt, int delay1, int delay2) throws InterruptedException {
        Dataflow df = new Dataflow();
        Utils.CurrentThreadExecutor executor = new Utils.CurrentThreadExecutor();
        df.setExecutor(executor);
        PublisherActor pub = new PublisherActor(df, cnt, delay1);
        pub.start();
        PublisherActor pub2 = new PublisherActor(df, cnt, delay1);
        pub2.start();
        ScalarSubscriberActor sub = new ScalarSubscriberActor(df, delay2, cnt*2, pub, pub2);
        sub.start();
        executor.executeAll();
        boolean res = df.isCompleted();
        Assert.assertTrue(res);
        Assert.assertEquals(0, pub.cnt);
        Assert.assertEquals(0, pub2.cnt);
        Assert.assertEquals(0, sub.cnt);
    }

    @Test
    public void pubSubTest() throws InterruptedException {
        pubSubTest(0, 0, 0);
    }

    @Test
    public void slowPubSubTest() throws InterruptedException {
        pubSubTest(0, 100, 0);
    }

    @Test
    public void slowPubTest() throws InterruptedException {
        pubSubTest(3, 100, 0);
    }

    @Test
    public void pubSlowSubConnTest() throws InterruptedException {
        pubSubTest(0, 0, 100);
    }

    @Test
    public void pubSlowSubTest() throws InterruptedException {
        pubSubTest(4, 0, 100);
    }
}

