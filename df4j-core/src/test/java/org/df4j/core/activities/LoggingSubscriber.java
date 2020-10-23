package org.df4j.core.activities;

import org.df4j.core.connector.Completion;
import org.df4j.core.util.LoggerFactory;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;

import java.util.logging.Level;

public class LoggingSubscriber extends Completion implements Subscriber<Long> {
    public final Logger logger = LoggerFactory.getLogger(this);
    public int cnt = 0;
    public Subscription subscription;
    public Long in;

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(Long in) {
        cnt++;
        this.in = in;
        logger.info(" got: " + in);
    }

    @Override
    public void onError(Throwable e) {
        logger.info(" completed with: " + e);
        super.completeExceptionally(e);
    }

    @Override
    public void onComplete() {
        logger.info(" completed normally");
        super.complete();
    }
}
