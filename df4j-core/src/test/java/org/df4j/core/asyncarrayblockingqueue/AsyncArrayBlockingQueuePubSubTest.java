package org.df4j.core.asyncarrayblockingqueue;

import org.df4j.core.activities.ProducerThread;
import org.df4j.core.activities.SubscriberActor;
import org.df4j.core.connector.AsyncArrayBlockingQueue;
import org.df4j.core.dataflow.Dataflow;
import org.junit.Assert;
import org.junit.Test;

import java.util.logging.Level;

public class AsyncArrayBlockingQueuePubSubTest {

    public void testAsyncQueue(int cnt, int delay1, int delay2) {
        Dataflow graph = new Dataflow();
        AsyncArrayBlockingQueue<Long> queue = new AsyncArrayBlockingQueue<>(3);
        ProducerThread producer = new ProducerThread(cnt, queue, delay1);
        SubscriberActor subscriber = new SubscriberActor(graph, delay2);
        queue.subscribe(subscriber.inp);
        producer.start();
        subscriber.start();
        boolean fin = graph.blockingAwait(100);
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
    public void testAsyncQueueCons() throws InterruptedException {
        int cnt = 3;
        AsyncArrayBlockingQueue<Long> queue = new AsyncArrayBlockingQueue<Long>(cnt);
        for (long k = cnt; k>0; k--) {
            queue.add(k);
        }
        SubscriberActor subscriber = new SubscriberActor(0);
        subscriber.setLogLevel(Level.ALL);
        queue.subscribe(subscriber.inp);
        subscriber.start();
        queue.onComplete();
        boolean fin = subscriber.blockingAwait(100);
        Assert.assertTrue(fin);
    }
}


