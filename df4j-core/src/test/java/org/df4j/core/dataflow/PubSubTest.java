package org.df4j.core.dataflow;

import org.df4j.core.actors.Publisher;
import org.df4j.core.actors.Subscriber;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Flow;

public class PubSubTest {

    static class Observer implements Flow.Subscriber<Integer> {
        volatile boolean completed = false;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Integer.MAX_VALUE);
        }

        @Override
        public void onNext(Integer in) {
            System.out.println(" got: " + in);
        }

        @Override
        public void onError(Throwable e) {
            System.out.println(" completed with: " + e);
            completed = true;
        }

        @Override
        public void onComplete() {
            onError(null);
        }
    }

    @Test
    public void pubOnlyTest() throws InterruptedException {
        Publisher pub = new Publisher(3, 0);
        Observer sub = new Observer();
        pub.start();
        pub.out.subscribe(sub);

        pub.blockingAwait(100);
        boolean alive = pub.isAlive();
        Assert.assertFalse(alive);
        Assert.assertTrue(sub.completed);
    }

    public void pubSubTest(int cnt, int delay1, int delay2) throws InterruptedException {
        Publisher pub = new Publisher(cnt, delay1);
        Subscriber sub = new Subscriber(pub.out, delay2);
        pub.start();
        sub.start();
        sub.blockingAwait(1000);
        pub.blockingAwait(1000);
        Assert.assertFalse(sub.isAlive());
        Assert.assertFalse(pub.isAlive());
    }

    @Test
    public void slowPubConnTest() throws InterruptedException {
        pubSubTest(0, 100, 0);
    }

    @Test
    public void slowPubTest() throws InterruptedException {
        pubSubTest(3, 100, 0);
    }

    @Test
    public void pubSlowSubConnTest() throws InterruptedException {
        pubSubTest(0, 0, 100);
    }

    @Test
    public void pubSlowSubTest() throws InterruptedException {
        pubSubTest(4, 0, 100);
    }
}

