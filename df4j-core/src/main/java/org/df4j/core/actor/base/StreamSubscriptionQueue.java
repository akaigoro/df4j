package org.df4j.core.actor.base;

import org.df4j.core.protocols.Flow;
import org.df4j.core.util.linked.Link;
import org.df4j.core.util.linked.LinkedQueue;

import java.util.concurrent.locks.ReentrantLock;

/**
 * non-blocking queue of {@link FlowSubscriptionImpl}
 *
 * @param <T>  type of tokens
 */
public abstract class StreamSubscriptionQueue<T> {
    protected final ReentrantLock locker = new ReentrantLock();
    protected LinkedQueue<FlowSubscriptionImpl> activeSubscriptions = new LinkedQueue<>();
    protected LinkedQueue<FlowSubscriptionImpl> passiveSubscriptions = new LinkedQueue<>();
    protected volatile boolean completed = false;
    protected volatile boolean completionRequested = false;
    protected Throwable completionException;

    public void subscribe(Flow.Subscriber<? super T> s) {
        FlowSubscriptionImpl subscription = new FlowSubscriptionImpl(s);
        subscription.lazyMode = true;
        try {
            subscription.subscriber.onSubscribe(subscription);
        } catch (Throwable thr) {
            subscription.subscriber.onError(thr);
        }
        subscription.lazyMode = false;
        locker.lock();
        try {
            if (completed) {
                subscription.complete();
            } else {
                storeSubscription(subscription);
                matchingLoop();
            }
        } finally {
            locker.unlock();
        }
    }

    protected void matchingLoop() {
        while (hasNextToken() && !activeSubscriptions.isEmpty()) {
            T token = nextToken();
            FlowSubscriptionImpl subscription = activeSubscriptions.poll();
            Flow.Subscriber subscriber = subscription.subscriber;
            subscription.requested--;
            subscription.lazyMode = true;
            locker.unlock();
            try {
                subscriber.onNext(token);
            } catch (Throwable thr) {
                subscriber.onError(thr);
            }
            locker.lock();
            subscription.lazyMode = false;
            storeSubscription(subscription);
        }
        checkCompletion();
    }

    private void checkCompletion() {
        if (completionRequested && !completed && !hasNextToken()) {
            completed = true;
            completeSubscriptions(activeSubscriptions);
            completeSubscriptions(passiveSubscriptions);
        }
    }

    private void storeSubscription(FlowSubscriptionImpl subscription) {
        if (!subscription.isCancelled()) {
            if (subscription.requested > 0) {
                activeSubscriptions.offer(subscription);
            } else {
                passiveSubscriptions.offer(subscription);
            }     
        }
    }

    private void completeSubscriptions(LinkedQueue<FlowSubscriptionImpl> subscriptions) {
        for (;;) {
            FlowSubscriptionImpl subscription = subscriptions.poll();
            if (subscription == null) {
                break;
            }
            subscription.complete();
        }
    }

    public void onComplete () {
        completion(null);
    }

    public void onError (Throwable throwable){
        if (throwable != null) {
            throw  new IllegalArgumentException();
        }
        completion(throwable);
    }

    public void completion(Throwable completionException) {
        locker.lock();
        try {
            if (completionRequested) {
                return;
            }
            completionRequested = true;
            this.completionException = completionException;
            checkCompletion();
        } finally {
            locker.unlock();
        }
    }

    protected abstract boolean hasNextToken();

    protected abstract T nextToken ();

    public class FlowSubscriptionImpl extends Link<FlowSubscriptionImpl> implements Flow.Subscription {
        protected long requested = 0;
        private Flow.Subscriber subscriber;
        private volatile boolean lazyMode = false;

        public FlowSubscriptionImpl(Flow.Subscriber subscriber) {
            this.subscriber = subscriber;
        }

        public boolean isCancelled() {
            return subscriber == null;
        }

        public boolean isActive() {
            return requested > 0;
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                subscriber.onError(new IllegalArgumentException());
                return;
            }
            locker.lock();
            try {
                if (isCancelled()) {
                    return;
                }
                boolean wasActive = isActive();
                requested += n;
                if (requested < 0) { // long arithmetic overflow
                    requested = Long.MAX_VALUE;
                }
                if (wasActive) {
                    return;
                }
                unlink();
                if (lazyMode) {
                    return;
                }
                storeSubscription(this);
                matchingLoop();                
            } finally {
                locker.unlock();
            }
        }

        @Override
        public void cancel() {
            locker.lock();
            try {
                if (isCancelled()) {
                    return;
                }
                makeCancelled();
            } finally {
                locker.unlock();
            }
        }

        private void makeCancelled() {
            subscriber = null;
            unlink();
        }

        private void complete() {
            Flow.Subscriber subscriberLoc = subscriber;
            makeCancelled();
            locker.unlock();
            try {
                if (completionException == null) {
                    subscriberLoc.onComplete();
                } else {
                    subscriberLoc.onError(completionException);
                }
            } catch (Throwable thr) {
                subscriberLoc.onError(thr);
            }
            locker.lock();
        }
    }

}
