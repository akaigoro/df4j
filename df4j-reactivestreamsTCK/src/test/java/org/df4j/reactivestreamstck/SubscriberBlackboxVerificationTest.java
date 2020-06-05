package org.df4j.reactivestreamstck;

import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.TestEnvironment;

public class SubscriberBlackboxVerificationTest extends org.reactivestreams.tck.SubscriberBlackboxVerification {
    static final  int defaultTimeout = 400;

    protected SubscriberBlackboxVerificationTest() {
        super(new TestEnvironment(defaultTimeout));
    }

    @Override
    public Subscriber createSubscriber() {
        return new SubscriberActor(0).inp;
    }

    @Override
    public Object createElement(int element) {
        return element;
    }
}
