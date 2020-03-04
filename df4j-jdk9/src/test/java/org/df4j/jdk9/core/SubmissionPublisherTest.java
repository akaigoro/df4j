package org.df4j.jdk9.core;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

public class SubmissionPublisherTest {
    @Test
    public void test1() throws InterruptedException {
        int num = 4;
        MySubscriber sub = new MySubscriber(num);
        SubmissionPublisher<Integer> publisher = new SubmissionPublisher<>();
        for (int k=0; k<num; k++) {
            publisher.submit(k);
        }
        publisher.subscribe(sub);
        sub.count.await();
    }

    private class MySubscriber implements Flow.Subscriber<Integer> {
        Flow.Subscription sub;
        final CountDownLatch count;

        private MySubscriber(int num) {
            count = new CountDownLatch(num);
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            sub = subscription;
            subscription.request(1);
        }

        @Override
        public void onNext(Integer item) {
            count.countDown();
            if (count.getCount() == 0) {
                return;
            }
            sub.request(1);
        }

        @Override
        public void onError(Throwable throwable) {

        }

        @Override
        public void onComplete() {

        }
    }
}
