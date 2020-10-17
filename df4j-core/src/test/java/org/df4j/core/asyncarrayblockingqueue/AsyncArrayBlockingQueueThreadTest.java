package org.df4j.core.asyncarrayblockingqueue;

import org.df4j.core.activities.ConsumerThread;
import org.df4j.core.activities.ProducerThread;
import org.df4j.core.connector.AsyncArrayBlockingQueue;
import org.junit.Assert;
import org.junit.Test;

public class AsyncArrayBlockingQueueThreadTest {

    public void testAsyncQueue(int cnt, int delay1, int delay2) throws InterruptedException {
        AsyncArrayBlockingQueue queue = new AsyncArrayBlockingQueue<Integer>(3);
        ProducerThread producer = new ProducerThread(cnt, queue, delay1);
        ConsumerThread consumer = new ConsumerThread(queue, delay2);
        producer.start();
        consumer.start();
        consumer.join(400);
        Assert.assertTrue(consumer.blockingAwait(100));
    }

    @Test
    public void testAsyncQueueComplete() throws InterruptedException {
        testAsyncQueue(0,0, 0);
    }

    @Test
    public void testAsyncQueueSlowProdComplete() throws InterruptedException {
        testAsyncQueue(0,50, 0);
    }

    @Test
    public void testAsyncQueueFast() throws InterruptedException {
        testAsyncQueue(2,0, 0);
    }

    @Test
    public void testAsyncQueueSlowProd() throws InterruptedException {
        testAsyncQueue(5,50, 0);
    }

    @Test
    public void testAsyncQueueSlowCons() throws InterruptedException {
        testAsyncQueue(5,0, 50);
    }
}


