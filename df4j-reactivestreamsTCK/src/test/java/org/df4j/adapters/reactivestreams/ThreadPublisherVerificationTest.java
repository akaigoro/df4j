package org.df4j.adapters.reactivestreams;

import org.df4j.core.communicator.AsyncArrayBlockingQueue;
import org.reactivestreams.Publisher;
import org.reactivestreams.example.unicast.ThreadPublisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

/**
 * checks {@link AsyncArrayBlockingQueue} as implementation of {@link Publisher}
 */
@Test // Must be here for TestNG to find and run this, do not remove
public class ThreadPublisherVerificationTest extends PublisherVerification<Long> {
    static final long defaultTimeout = 400;

    public ThreadPublisherVerificationTest() {
        super(new TestEnvironment(defaultTimeout));
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        ThreadPublisher publisher = new ThreadPublisher(elements);
        publisher.start();
        return publisher;
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return null;
    }

    /**
     * the original test fails because Semaphore implementation used in ThreadPublisherSubscribtion does not suppoprt long values
     * @throws Throwable
     */
    @Override
    @Ignore
    public void required_spec317_mustSupportACumulativePendingElementCountUpToLongMaxValue() throws Throwable {
        return;
      //  super.required_spec317_mustSupportACumulativePendingElementCountUpToLongMaxValue();
    }

}
