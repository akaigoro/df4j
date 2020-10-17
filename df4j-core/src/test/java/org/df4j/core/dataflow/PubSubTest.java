package org.df4j.core.dataflow;

import org.df4j.core.activities.PublisherActor;
import org.df4j.core.activities.SubscriberActor;
import org.junit.Assert;
import org.junit.Test;

import java.util.logging.Level;

public class PubSubTest {

    public void pubSubTest(int cnt, int delay1, int delay2) {
        Dataflow df = new Dataflow();
        PublisherActor pub = new PublisherActor(df, cnt, delay1);
        SubscriberActor sub = new SubscriberActor(df, delay2);
        pub.out.subscribe(sub.inp);
        pub.start();
        sub.start();
        boolean res = df.blockingAwait(1000);
        Assert.assertTrue(res);
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

