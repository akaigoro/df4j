package org.df4j.core.asyncarrayblockingqueue;

import org.df4j.core.activities.ProducerActor;
import org.df4j.core.activities.SubscriberActor;
import org.df4j.core.communicator.AsyncArrayBlockingQueue;
import org.df4j.core.dataflow.Dataflow;
import org.junit.Assert;
import org.junit.Test;

public class AsyncArrayBlockingQueuePubSubTest {

    public void testAsyncQueue(int cnt, int delay1, int delay2) {
        Dataflow graph = new Dataflow();
        ProducerActor producer = new ProducerActor(graph, cnt, delay1);
        producer.start();
        SubscriberActor subscriber = new SubscriberActor(graph, delay2);
        subscriber.start();
        AsyncArrayBlockingQueue<Long> queue = new AsyncArrayBlockingQueue<>(3);
        queue.subscribe(subscriber.inp);
        queue.subscribe(producer.out);
        queue.complete();
        boolean fin = graph.blockingAwait(400);
        Assert.assertTrue(fin);
    }

    @Test
    public void testAsyncQueueProdSubComplete() {
        testAsyncQueue(0,0, 0);
    }

    @Test
    public void testAsyncQueueSlowProd() {
        testAsyncQueue(5,100, 0);
    }

    @Test
    public void testAsyncQueueSlowConsComplete() {
        testAsyncQueue(0,0, 100);
    }

    @Test
    public void testAsyncQueueSlowCons() {
        testAsyncQueue(3,0, 100);
    }

    @Test
    public void testAsyncQueueCompleted() {
        AsyncArrayBlockingQueue<Long> queue = new AsyncArrayBlockingQueue<Long>(1);
        queue.add(1l);
        queue.complete();
        Assert.assertFalse(queue.isCompleted());
        queue.remove();
        Assert.assertTrue(queue.isCompleted());
    }

    @Test
    public void testAsyncQueueCons() throws InterruptedException {
        int cnt = 3;
        AsyncArrayBlockingQueue<Long> queue = new AsyncArrayBlockingQueue<Long>(cnt);
        for (long k = cnt; k>0; k--) {
            queue.add(k);
        }
        SubscriberActor subscriber = new SubscriberActor(0);
        queue.subscribe(subscriber.inp);
        subscriber.start();
        Thread.sleep(400);
        queue.complete();
        Thread.sleep(400);
        subscriber.getParent().blockingAwait(400);
        boolean qIsCompleted = queue.isCompleted();
        Assert.assertTrue(qIsCompleted);
        SubscriberActor subscriber2 = new SubscriberActor(0);
        queue.subscribe(subscriber2.inp);
        subscriber2.start();
        boolean fin = subscriber.blockingAwait(400);
        boolean fin2 = subscriber2.blockingAwait(400);
        Assert.assertTrue(fin2);
        Assert.assertTrue(fin);
    }
}


