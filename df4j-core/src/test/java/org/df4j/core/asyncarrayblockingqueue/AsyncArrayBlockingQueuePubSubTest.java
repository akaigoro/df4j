package org.df4j.core.asyncarrayblockingqueue;

import org.df4j.core.activities.ProducerActor;
import org.df4j.core.activities.PublisherActor;
import org.df4j.core.activities.SubscriberActor;
import org.df4j.core.actor.ActorGroup;
import org.df4j.core.connector.AsyncArrayBlockingQueue;
import org.junit.Assert;
import org.junit.Test;

public class AsyncArrayBlockingQueuePubSubTest {

    public void testAsyncQueue(int cnt, int delay1, int delay2) {
        ActorGroup graph = new ActorGroup();
        ProducerActor producer = new ProducerActor(graph, cnt, delay1);
        PublisherActor publisher = new PublisherActor(graph, cnt, delay1);
        AsyncArrayBlockingQueue<Long> queue = new AsyncArrayBlockingQueue<>(3);
        queue.feedFrom(producer.out);
        queue.feedFrom(publisher.out);
        SubscriberActor subscriber = new SubscriberActor(graph, delay2);
        queue.subscribe(subscriber.inp);
        producer.start();
        subscriber.start();
        boolean fin = graph.await(1000);
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
        subscriber.getActorGroup().await(400);
        boolean qIsCompleted = queue.isCompleted();
        Assert.assertTrue(qIsCompleted);
        SubscriberActor subscriber2 = new SubscriberActor(0);
        queue.subscribe(subscriber2.inp);
        subscriber2.start();
        boolean fin = subscriber.await(400);
        boolean fin2 = subscriber2.await(400);
        Assert.assertTrue(fin2);
        Assert.assertTrue(fin);
    }
}


