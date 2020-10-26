package org.df4j.core.communicator;

import org.df4j.core.connector.AsyncCountDownLatch;
import org.df4j.protocol.Completable;
import org.df4j.protocol.SimpleSubscription;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncCountdownLatchTest {
    @Test
    public void test0() {
        AsyncCountDownLatch latch = new AsyncCountDownLatch(0);
        MySubscriber subscriber = new MySubscriber();
        latch.subscribe(subscriber);
        Assert.assertFalse(subscriber.onSubscribeSeen.get());
        Assert.assertTrue(subscriber.onCompleteSeen.get());
    }

    @Test
    public void test1() {
        AsyncCountDownLatch latch = new AsyncCountDownLatch(1);
        MySubscriber subscriber = new MySubscriber();
        latch.subscribe(subscriber);
        Assert.assertTrue(subscriber.onSubscribeSeen.get());
        Assert.assertFalse(subscriber.onCompleteSeen.get());
        latch.countDown();
        Assert.assertTrue(subscriber.onCompleteSeen.get());
    }

    private static class MySubscriber implements Completable.Subscriber {
        AtomicBoolean onSubscribeSeen = new AtomicBoolean(false);
        AtomicBoolean onCompleteSeen = new AtomicBoolean(false);

        @Override
        public void onSubscribe(SimpleSubscription subscription) {
            onSubscribeSeen.set(true);
        }

        @Override
        public void onError(Throwable e) {
            throw new AssertionError(e);
        }

        @Override
        public void onComplete() {
            onCompleteSeen.set(true);
        }
    }
}
