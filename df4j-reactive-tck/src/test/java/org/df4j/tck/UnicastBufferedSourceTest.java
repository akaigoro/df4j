package org.df4j.tck;


import org.df4j.core.actor.Logger;
import org.df4j.core.actor.Source;
import org.df4j.core.actor.UnicastBufferedSource;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.Test;

public class UnicastBufferedSourceTest extends PublisherVerification {
    static final  int defaultTimeout = 400;

    public UnicastBufferedSourceTest() throws NoSuchFieldException, IllegalAccessException {
        super(defaultTimeout);
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        Logger parent = new Logger(true);
        Source flowPublisher = new UnicastBufferedSource(parent, elements);
        flowPublisher.start();
        return flowPublisher;
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        Logger parent = new Logger(true);
        Publisher<Long> flowPublisher = new FailedUnicastSource(parent);
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

    static class FailedUnicastSource extends UnicastBufferedSource {

        public FailedUnicastSource(Logger parent) {
            super(parent, 1);
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
