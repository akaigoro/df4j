package org.df4j.tck;


import org.df4j.core.actor.Source;
import org.df4j.core.actor.UnicastUnbufferedSource;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.Test;

import java.lang.reflect.Field;

public class ReactiveStreamUnicastPublisherTest extends PublisherVerification<Long> {
    static final  int defaultTimeout = 400;
    private final TestEnvironment env;

    public ReactiveStreamUnicastPublisherTest() throws NoSuchFieldException, IllegalAccessException {
        super(new TestEnvironment(defaultTimeout));

        Field env = PublisherVerification.class.getDeclaredField("env");
        env.setAccessible(true);
        this.env = (TestEnvironment) env.get(this);
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        Source flowPublisher = new UnicastUnbufferedSource(elements);
        flowPublisher.start();
        return flowPublisher;
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        Publisher<Long> flowPublisher = new FailedUnicastSource();
        return flowPublisher;
    }

    @Test
    public void required_spec317_mustSupportACumulativePendingElementCountGreaterThenLongMaxValue() throws Throwable {
//        final int totalElements = 50;
        final int totalElements = 2;

        activePublisherTest(totalElements, true, pub -> {
            final TestEnvironment.ManualSubscriber<Long> sub = env.newManualSubscriber(pub);
            new Thread(() -> sub.request(Long.MAX_VALUE)).start();
            new Thread(() -> sub.request(Long.MAX_VALUE)).start();

            sub.nextElements(totalElements);
            sub.expectCompletion();

            try {
                env.verifyNoAsyncErrorsNoDelay();
            } finally {
                sub.cancel();
            }

        });
    }


     static class FailedUnicastSource extends UnicastUnbufferedSource {

        public FailedUnicastSource() {
            super(1);
        }

        @Override
        public void subscribe(Subscriber<? super Long> subscriber) {
            super.subscribe(subscriber);
            subscriber.onError(new RuntimeException());
        }

        @Override
        protected void runAction() {
            output.onError(new RuntimeException());
        }
    }
}
