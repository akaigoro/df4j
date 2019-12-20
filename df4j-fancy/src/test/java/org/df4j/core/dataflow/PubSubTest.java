package org.df4j.core.dataflow;

import org.df4j.core.dataflow.actors.Publisher;
import org.df4j.core.dataflow.actors.Subscriber;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Flow;

public class PubSubTest {

    static class Observer implements Flow.Subscriber<Integer> {
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
        }

        @Override
        public void onComplete() {
            onError(null);
        }
    }

    @Test
    public void pubTest() throws InterruptedException {
        Publisher pub = new Publisher(3, 0);
        Observer sub = new Observer();
        pub.awake();
        while (!pub.isCompleted()) {
            pub.out.subscribe(sub);
        }
        pub.blockingAwait(100);
        Assert.assertFalse(pub.isAlive());
    }

    public void pubSubTest(int delay1, int delay2) throws InterruptedException {
        Publisher pub = new Publisher(3, delay1);
        Subscriber sub = new Subscriber(pub.out, delay2);
        pub.awake();
        sub.awake();
        sub.blockingAwait(1000);
        pub.blockingAwait(1000);
        Assert.assertFalse(sub.isAlive());
        Assert.assertFalse(pub.isAlive());
    }

    @Test
    public void slowPubTest() throws InterruptedException {
        pubSubTest(100, 0);
    }

    @Test
    public void pubSlowSubTest() throws InterruptedException {
        pubSubTest( 0, 100);
    }
}

