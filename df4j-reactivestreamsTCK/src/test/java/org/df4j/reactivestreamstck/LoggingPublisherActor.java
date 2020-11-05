package org.df4j.reactivestreamstck;

import org.df4j.core.actor.AbstractPublisher;
import org.df4j.core.actor.Actor;
import org.df4j.core.actor.ActorGroup;
import org.df4j.core.port.OutFlow;
import org.df4j.core.util.LoggerFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;

public class LoggingPublisherActor extends AbstractPublisher<Long> {
    protected final Logger logger = LoggerFactory.getLogger(this);
    final int delay;
    public long cnt;

    public LoggingPublisherActor(long elements) {
        this(elements, 0);
    }

    public LoggingPublisherActor(long cnt, int delay) {
        this(new ActorGroup(), cnt, delay);
    }

    public LoggingPublisherActor(ActorGroup parent, long cnt, int delay) {
        this(parent, cnt, delay, OutFlow.DEFAULT_CAPACITY);
    }

    public LoggingPublisherActor(ActorGroup parent, long cnt, int delay, int capacity) {
        super(parent);
        this.cnt = cnt;
        this.delay = delay;
        logger.info("PublisherActor: cnt = " + cnt);
    }

    @Override
    public void subscribe(Subscriber<? super Long> s) {
        logger.info("PublisherActor.subscribe:");
        super.subscribe(new ProxySubscriber(s));
    }

    @Override
    protected Long whenNext() throws Throwable {
        Thread.sleep(delay);
        if (cnt > 0) {
            logger.info("PublisherActor.onNext(" + cnt + ")");
            return cnt--;
        } else {
            logger.info("PublisherActor.onComplete");
            return null;
        }
    }

    private class ProxySubscription implements Subscription {
        private Subscription subscription;

        public ProxySubscription(Subscription subscription) {
            this.subscription = subscription;
        }

        @Override
        public void request(long n) {
            logger.info("    Subscription.request:" + n);
            subscription.request(n);
        }

        @Override
        public void cancel() {
            logger.info("    Subscription.cancel:");
            subscription.cancel();
        }
    }

    class ProxySubscriber implements Subscriber<Long> {
        private final Subscriber<? super Long> sub;

        public ProxySubscriber(Subscriber<? super Long> s) {
            sub = s;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            logger.info("        Subscriber.onSubscribe");
            ProxySubscription proxy = new ProxySubscription(subscription);
            sub.onSubscribe(proxy);
        }

        @Override
        public void onError(Throwable t) {
            logger.info("        Subscriber.onError:" + t);
            sub.onError(t);
        }

        @Override
        public void onComplete() {
            logger.info("        Subscriber.onComplete");
//                new Exception().printStackTrace();
            sub.onComplete();
        }

        @Override
        public void onNext(Long t) {
            logger.info("        Subscriber.onNext:" + t);
            sub.onNext(t);
        }
    }
}
