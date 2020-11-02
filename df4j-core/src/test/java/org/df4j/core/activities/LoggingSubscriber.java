package org.df4j.core.activities;

import org.df4j.core.connector.Completion;
import org.df4j.core.util.LoggerFactory;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;

public class LoggingSubscriber extends Completion implements Subscriber<Long> {
    public final Logger logger = LoggerFactory.getLogger(this);
    public int cnt = 0;
    public Subscription subscription;

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(Long in) {
        cnt++;
        logger.info(" got: " + in);
    }

    @Override
    public void onError(Throwable e) {
        logger.info(" completed with: " + e);
        super.complete(e);
    }

    @Override
    public void onComplete() {
        logger.info(" completed normally");
        super.complete();
    }
}
