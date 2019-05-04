package org.df4j.tck;


import org.df4j.core.actor.Logger;
import org.df4j.core.actor.Source;
import org.df4j.core.actor.UnicastBufferedSource;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.Test;

import java.lang.reflect.Field;

public class UnicastBufferedSourceTest extends org.reactivestreams.tck.PublisherVerification<Long> {
    static final  int defaultTimeout = 400;
    protected final TestEnvironment env;

    public UnicastBufferedSourceTest() throws NoSuchFieldException, IllegalAccessException {
        super(new TestEnvironment(defaultTimeout));
        Field fenv = org.reactivestreams.tck.PublisherVerification.class.getDeclaredField("env");
        fenv.setAccessible(true);
        this.env = (TestEnvironment) fenv.get(this);
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
        final int totalElements = 50;

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

 //   @Test
    public void required_spec107_mustNotEmitFurtherSignalsOnceOnCompleteHasBeenSignalledN() throws Throwable {
        for (int n=0; n<100; n++) {
            try {
                required_spec107_mustNotEmitFurtherSignalsOnceOnCompleteHasBeenSignalled();
            } catch (Throwable throwable) {
                System.err.println("n="+n);
                throw  throwable;
            }
        }
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
