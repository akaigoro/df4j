package org.df4j.core.asyncarrayblockingqueue;

import org.df4j.core.activities.ConsumerThread;
import org.df4j.core.activities.ProducerThread;
import org.df4j.core.communicator.AsyncArrayBlockingQueue;
import org.junit.Assert;
import org.junit.Test;

import java.util.logging.Level;

public class AsyncArrayBlockingQueueThreadTest {

    public void testAsyncQueue(int cnt, int delay1, int delay2) throws InterruptedException {
        AsyncArrayBlockingQueue queue = new AsyncArrayBlockingQueue<Integer>(3);
        ProducerThread producer = new ProducerThread(cnt, queue, delay1);
        ConsumerThread consumer = new ConsumerThread(queue, delay2);
        producer.logger.setLevel(Level.ALL);
        producer.start();
        consumer.logger.setLevel(Level.ALL);
    //    consumer.start();
        consumer.join(400);
        Assert.assertFalse(consumer.isAlive());
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


