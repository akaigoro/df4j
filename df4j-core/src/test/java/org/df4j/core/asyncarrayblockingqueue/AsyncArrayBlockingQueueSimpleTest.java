package org.df4j.core.asyncarrayblockingqueue;

import org.df4j.core.activities.LoggingSubscriber;
import org.df4j.core.communicator.AsyncArrayBlockingQueue;
import org.junit.Assert;
import org.junit.Test;

import java.util.logging.Level;

public class AsyncArrayBlockingQueueSimpleTest {

    public void pubSubTest(int cnt) {
        AsyncArrayBlockingQueue<Long> queue = new AsyncArrayBlockingQueue<>(3);
        LoggingSubscriber sub = new LoggingSubscriber();
        sub.logger.setLevel(Level.ALL);
        queue.subscribe(sub);
        for (long k=0; k<cnt; k++) {
            queue.add(k);
        }
        queue.onComplete();
        Assert.assertEquals(cnt, sub.cnt);
        Assert.assertTrue(sub.completed);
        Assert.assertNull(sub.completionException);
    }

    @Test
    public void pubSubTest0() throws InterruptedException {
        pubSubTest(0);
    }

    @Test
    public void pubSubTest1() throws InterruptedException {
        pubSubTest(1);
    }

    @Test
    public void pubSubTest4() throws InterruptedException {
        pubSubTest(4);
    }

    @Test
    public void cancelTest() {
        int cnt = 4;
        AsyncArrayBlockingQueue<Long> queue = new AsyncArrayBlockingQueue<>(3);
        LoggingSubscriber sub = new LoggingSubscriber();
        sub.logger.setLevel(Level.ALL);
        queue.subscribe(sub);
        queue.add(0L);
        Assert.assertEquals(1, sub.cnt);
        Assert.assertFalse(sub.completed);
        Assert.assertNull(sub.completionException);
        sub.subscription.cancel();
        queue.onComplete();
        for (long k=0; ; k++) {
            if (!queue.offer(k)) {
                break;
            }
        }
        Assert.assertEquals(1, sub.cnt);
        Assert.assertFalse(sub.completed);
    }

}


