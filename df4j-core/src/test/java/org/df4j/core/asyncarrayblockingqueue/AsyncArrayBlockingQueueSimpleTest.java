package org.df4j.core.asyncarrayblockingqueue;

import org.df4j.core.activities.LoggingSubscriber;
import org.df4j.core.communicator.AsyncArrayBlockingQueue;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CompletionException;

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
        queue.onComplete();
        Assert.assertFalse(queue.isCompleted());
        for (long k=0; k<cnt; k++) {
            Long value = queue.remove();
            Assert.assertEquals(k, value.longValue());
        }
        boolean condition = queue.blockingAwait(100);
        Assert.assertTrue(condition);
        try {
            queue.remove();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof CompletionException);
        }
    }

    @Test
    public void addTest1() throws InterruptedException {
        addRemoveTest(1);
    }

    @Test
    public void addTest4() throws InterruptedException {
        addRemoveTest(4);
    }

    public void addSubTest(int cnt) throws InterruptedException {
        AsyncArrayBlockingQueue<Long> queue = new AsyncArrayBlockingQueue<>(cnt);
        for (long k=0; k<cnt; k++) {
            queue.add(k);
        }
//        queue.onComplete();
        queue.onComplete();
        Assert.assertFalse(queue.isCompleted());
        for (long k=0; k<cnt; k++) {
            Long item = queue.remove();
            Assert.assertEquals(k, item.longValue());
        }
        Assert.assertTrue(queue.blockingAwait(100));
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
    public void cancelTest() throws InterruptedException {
        int cnt = 4;
        AsyncArrayBlockingQueue<Long> queue = new AsyncArrayBlockingQueue<>(3);
        LoggingSubscriber sub = new LoggingSubscriber();
        queue.subscribe(sub);
        sub.subscription.cancel();
        queue.onComplete();
        Assert.assertTrue(queue.blockingAwait(100));
        Assert.assertFalse(sub.blockingAwait(100));
    }

    @Test
    public void completelTest() throws InterruptedException {
        int cnt = 4;
        AsyncArrayBlockingQueue<Long> queue = new AsyncArrayBlockingQueue<>(3);
        LoggingSubscriber sub = new LoggingSubscriber();
        queue.subscribe(sub);
        Assert.assertFalse(sub.blockingAwait(100));
        queue.onComplete();
        boolean condition = queue.blockingAwait(100);
        Assert.assertTrue(condition);
        Assert.assertTrue(sub.blockingAwait(100));
    }

    @Test
    public void testAsyncQueueCompleted() {
        AsyncArrayBlockingQueue<Long> queue = new AsyncArrayBlockingQueue<Long>(1);
        queue.add(1l);
        queue.onComplete();
        Assert.assertFalse(queue.isCompleted());
        queue.remove();
        boolean condition = queue.blockingAwait(100);
        Assert.assertTrue(condition);
    }

}


