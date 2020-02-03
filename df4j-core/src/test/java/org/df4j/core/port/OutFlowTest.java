package org.df4j.core.port;

import org.df4j.core.activities.LoggingSubscriber;
import org.df4j.core.activities.PublisherActor;
import org.df4j.core.util.Utils;
import org.junit.Assert;
import org.junit.Test;

public class OutFlowTest {

    @Test
    public void outFlowTest() throws InterruptedException {
        Utils.CurrentThreadExecutor executor = new Utils.CurrentThreadExecutor();
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

