package org.df4j.core.port;

import org.df4j.core.activities.PublisherActor;
import org.df4j.protocol.Flow;
import org.junit.Assert;
import org.junit.Test;

public class OutFlowTest {

    static class SimpleSubscriber implements Flow.Subscriber<Integer> {
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
    public void outFlowTest() throws InterruptedException {
        PublisherActor pub = new PublisherActor(1, 0);
        SimpleSubscriber simpleSubscriber = new SimpleSubscriber();
        pub.out.subscribe(simpleSubscriber);
        pub.start();

        boolean success = pub.blockingAwait(100);
        Assert.assertTrue(success);
        Thread.sleep(50);
        Assert.assertTrue(simpleSubscriber.completed);
    }
}

