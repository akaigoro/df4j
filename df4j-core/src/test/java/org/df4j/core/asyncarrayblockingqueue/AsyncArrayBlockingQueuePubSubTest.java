package org.df4j.core.asyncarrayblockingqueue;

import org.df4j.core.activities.ProducerActor;
import org.df4j.core.activities.SubscriberActor;
import org.df4j.core.communicator.AsyncArrayBlockingQueue;
import org.junit.Assert;
import org.junit.Test;

public class AsyncArrayBlockingQueuePubSubTest {

    public void testAsyncQueue(int cnt, int delay1, int delay2) throws InterruptedException {
        ProducerActor producer = new ProducerActor(cnt, delay1);
        SubscriberActor subscriber = new SubscriberActor(delay2);
        AsyncArrayBlockingQueue<Long> queue = new AsyncArrayBlockingQueue<>(3);
        queue.subscribe(producer.out);
        queue.subscribe(subscriber.inp);
        producer.start();
        subscriber.start();
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
        AsyncArrayBlockingQueue<Long> queue = new AsyncArrayBlockingQueue<Long>(cnt);
        for (long k = cnt; k>0; k--) {
            queue.offer(k);
        }
        SubscriberActor subscriber = new SubscriberActor(0);
        queue.subscribe(subscriber.inp);
        subscriber.start();
        queue.onComplete();
        //   producer.join();
        boolean fin = subscriber.blockingAwait(1000);
        Assert.assertTrue(fin);
    }
}


