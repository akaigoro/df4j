package org.df4j.core.activities;

import org.df4j.core.util.Logger;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class LoggingSubscriber implements Subscriber<Long> {
    public final Logger logger = new Logger(this);
    public int cnt = 0;
    public Subscription subscription;
    public Long in;
    public volatile boolean completed = false;
    public volatile Throwable completionException = null;

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
        completed = true;
        completionException = e;
    }

    @Override
    public void onComplete() {
        onError(null);
    }
}
