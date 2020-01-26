package org.df4j.core.port;

import org.df4j.core.activities.PublisherActor;
import org.df4j.core.util.Logger;
import org.df4j.core.util.Utils;
import org.reactivestreams.*;
import org.junit.Assert;
import org.junit.Test;

public class OutFlowTest {

    static class SimpleSubscriber implements Subscriber<Long> {
        protected final Logger logger = new Logger(this);
        Subscription subscription;
        Long in;
        volatile boolean completed = false;

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(Long in) {
            this.in = in;
            logger.info(" got: " + in);
        }

        @Override
        public void onError(Throwable e) {
            logger.info(" completed with: " + e);
            completed = true;
        }

        @Override
        public void onComplete() {
            onError(null);
        }
    }

    @Test
    public void outFlowTest() throws InterruptedException {
        Utils.CurrentThreadExecutor executor = new Utils.CurrentThreadExecutor();
        PublisherActor pub = new PublisherActor(1, 0);
        pub.setExecutor(executor);
        SimpleSubscriber sub = new SimpleSubscriber();
        pub.out.subscribe(sub);
        pub.start();
        executor.executeAll();
        boolean success = pub.blockingAwait(0);
        Assert.assertTrue(success);
        Thread.sleep(50);
        Assert.assertTrue(sub.completed);
    }
}

