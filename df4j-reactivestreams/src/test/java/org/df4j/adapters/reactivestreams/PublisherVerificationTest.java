package org.df4j.adapters.reactivestreams;

import org.df4j.core.activities.ReactivePublisher;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.TestEnvironment;

import java.lang.reflect.Field;

public class PublisherVerificationTest  extends org.reactivestreams.tck.PublisherVerification<Long> {
    static final  int defaultTimeout = 400;
    protected final TestEnvironment env;

    public PublisherVerificationTest() throws NoSuchFieldException, IllegalAccessException {
        super(new TestEnvironment(defaultTimeout));
        Field fenv = org.reactivestreams.tck.PublisherVerification.class.getDeclaredField("env");
        fenv.setAccessible(true);
        this.env = (TestEnvironment) fenv.get(this);
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        ReactivePublisher publisher = new ReactivePublisher(elements);
        publisher.start();
        return publisher.out;
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        ReactivePublisher publisher = new ReactivePublisher(0){
            @Override
            protected void runAction() {
                out.onError(new RuntimeException());
                stop();
            }
        };
        publisher.start();
        return publisher.out;
    }

    public void failingtets() throws Throwable {
        super.stochastic_spec103_mustSignalOnMethodsSequentially();
    }
}
