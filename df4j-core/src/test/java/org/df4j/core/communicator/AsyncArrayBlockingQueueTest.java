package org.df4j.core.communicator;

import org.df4j.core.actors.Producer;
import org.df4j.core.actors.Subscriber;
import org.junit.Assert;
import org.junit.Test;

public class AsyncArrayBlockingQueueTest {

    public void testAsyncQueue(int cnt, int delay1, int delay2) throws InterruptedException {
        AsyncArrayBlockingQueue queue = new AsyncArrayBlockingQueue<Integer>(3);
        Producer producer = new Producer(cnt, queue, delay1);
        Subscriber subscriber = new Subscriber(queue, delay2);
        producer.start();
        subscriber.start();
     //   producer.join();
        boolean fin = subscriber.blockingAwait(1000);
        Assert.assertTrue(fin);
    }

    @Test
    public void testAsyncQueueComplete() throws InterruptedException {
        testAsyncQueue(0,0, 0);
    }

    @Test
    public void testAsyncQueueSlowProd() throws InterruptedException {
        testAsyncQueue(5,100, 0);
    }

    @Test
    public void testAsyncQueueSlowConsComplete() throws InterruptedException {
        testAsyncQueue(0,0, 100);
    }

    @Test
    public void testAsyncQueueSlowCons() throws InterruptedException {
        testAsyncQueue(3,0, 100);
    }

    @Test
    public void testAsyncQueueCons() throws InterruptedException {
        int cnt = 3;
        AsyncArrayBlockingQueue<Integer> queue = new AsyncArrayBlockingQueue<Integer>(cnt);
        for (int k = cnt; k>0; k--) {
            queue.offer(k);
        }
        Subscriber subscriber = new Subscriber(queue, 0);
        subscriber.start();
        queue.onComplete();
        //   producer.join();
        boolean fin = subscriber.blockingAwait(1000);
        Assert.assertTrue(fin);
    }
}


