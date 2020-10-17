package org.df4j.core.asyncarrayblockingqueue;

import org.df4j.core.activities.LoggingSubscriber;
import org.df4j.core.connector.AsyncArrayBlockingQueue;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CompletionException;

import static org.junit.Assert.fail;

public class AsyncArrayBlockingQueueSimpleTest {

    public void addRemoveTest(int cnt) {
        AsyncArrayBlockingQueue<Long> queue = new AsyncArrayBlockingQueue<>(cnt);
        for (long k=0; k<cnt; k++) {
            Assert.assertEquals(k, queue.size());
            queue.add(k);
        }
        try {
            queue.add(0l);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalStateException);
        }
        Assert.assertEquals(cnt, queue.size());
        Assert.assertFalse(queue.isCompleted());
        queue.complete();
        Assert.assertEquals(queue.size()==0, queue.isCompleted());
        for (long k=0; k<cnt; k++) {
            Long value = queue.remove();
            Assert.assertEquals(k, value.longValue());
        }
        Assert.assertTrue(queue.isCompleted());
        try {
            queue.remove();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof CompletionException);
        }
    }

    @Test
    public void addTest2() throws InterruptedException {
        addRemoveTest(2);
    }

    @Test
    public void addTest4() throws InterruptedException {
        addRemoveTest(4);
    }

    public void addSubTest(int cnt) {
        AsyncArrayBlockingQueue<Long> queue = new AsyncArrayBlockingQueue<>(3);
        LoggingSubscriber sub = new LoggingSubscriber();
        queue.subscribe(sub);
        for (long k=0; k<cnt; k++) {
            queue.add(k);
        }
        queue.complete();
        Assert.assertTrue(queue.isCompleted());
        Assert.assertEquals(cnt, sub.cnt);
        Assert.assertTrue(sub.isCompleted());
        Assert.assertNull(sub.getCompletionException());
    }

    @Test
    public void pubSubTest0() throws InterruptedException {
        addSubTest(0);
    }

    @Test
    public void pubSubTest1() throws InterruptedException {
        addSubTest(1);
    }

    @Test
    public void pubSubTest4() throws InterruptedException {
        addSubTest(4);
    }

    @Test
    public void cancelTest() {
        int cnt = 4;
        AsyncArrayBlockingQueue<Long> queue = new AsyncArrayBlockingQueue<>(3);
        LoggingSubscriber sub = new LoggingSubscriber();
        queue.subscribe(sub);
        queue.offer(0L);
        Assert.assertEquals(1, sub.cnt);
        Assert.assertFalse(sub.isCompleted());
        Assert.assertNull(sub.getCompletionException());
        sub.subscription.cancel();
        queue.complete();
        for (long k=0; ; k++) {
            if (!queue.offer(k)) {
                break;
            }
        }
        Assert.assertEquals(1, sub.cnt);
        Assert.assertFalse(sub.isCompleted());
    }

}


