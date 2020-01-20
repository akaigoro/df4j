package org.df4j.core.communicator;

import org.df4j.protocol.Completable;
import org.df4j.protocol.SimpleSubscription;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncCountdownLatchTest {
    @Test
    public void test0() {
        AsyncCountDownLatch latch = new AsyncCountDownLatch(0);
        MyObserver subscriber = new MyObserver();
        latch.subscribe(subscriber);
        Assert.assertFalse(subscriber.onSubscribeSeen.get());
        Assert.assertTrue(subscriber.onCompleteSeen.get());
    }

    @Test
    public void test1() {
        AsyncCountDownLatch latch = new AsyncCountDownLatch(1);
        MyObserver subscriber = new MyObserver();
        latch.subscribe(subscriber);
        Assert.assertTrue(subscriber.onSubscribeSeen.get());
        Assert.assertFalse(subscriber.onCompleteSeen.get());
        latch.countDown();
        Assert.assertTrue(subscriber.onCompleteSeen.get());
    }

    private static class MyObserver implements Completable.Observer {
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
