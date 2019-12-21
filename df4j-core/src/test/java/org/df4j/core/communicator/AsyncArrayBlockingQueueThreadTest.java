package org.df4j.core.communicator;

import org.df4j.core.threads.ProducerT;
import org.df4j.core.threads.ConsumerT;
import org.junit.Assert;
import org.junit.Test;

public class AsyncArrayBlockingQueueThreadTest {

    public void testAsyncQueue(int cnt, int delay1, int delay2) throws InterruptedException {
        AsyncArrayBlockingQueue queue = new AsyncArrayBlockingQueue<Integer>(3);
        ProducerT producer = new ProducerT(cnt, queue, delay1);
        ConsumerT subscriber = new ConsumerT(queue, delay2);
        producer.start();
        subscriber.start();
//        subscriber.join();
        subscriber.join(1000);
        Assert.assertFalse(subscriber.isAlive());
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
        testAsyncQueue(10,0, 0);
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


