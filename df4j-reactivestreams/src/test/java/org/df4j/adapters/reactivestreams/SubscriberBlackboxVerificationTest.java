package org.df4j.adapters.reactivestreams;

import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.TestEnvironment;

public class SubscriberBlackboxVerificationTest extends org.reactivestreams.tck.SubscriberBlackboxVerification {
    static final  int defaultTimeout = 400;

    protected SubscriberBlackboxVerificationTest() {
        super(new TestEnvironment(defaultTimeout));
    }

    @Override
    public Subscriber createSubscriber() {
        return null;
    }

    @Override
    public Object createElement(int element) {
        return null;
    }
}
