package org.df4j.core.actor;

import org.df4j.core.asyncproc.ScalarSubscriber;
import org.df4j.core.util.linked.Link;
import org.df4j.core.util.linked.LinkedQueue;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.locks.ReentrantLock;

/**
 * non-blocking queue of {@link StreamSubscription}
 *
 * @param <T>
 */
public abstract class StreamSubscriptionQueue<T> implements Publisher<T> {
    protected final ReentrantLock locker = new ReentrantLock();
    protected LinkedQueue<StreamSubscription> activeSubscriptions = new LinkedQueue<>();
    protected LinkedQueue<StreamSubscription> passiveSubscriptions = new LinkedQueue<>();
    protected volatile boolean completed = false;
    protected volatile boolean completionRequested = false;
    protected Throwable completionException;

    @Override
    public void subscribe(Subscriber<? super T> s) {
        StreamSubscription subscription = new StreamSubscription(s);
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

    public void subscribe (ScalarSubscriber < ? super T > s){
        Scalar2StreamSubscriber proxySubscriber = new Scalar2StreamSubscriber(s);
        subscribe(proxySubscriber);
    }

    protected void matchingLoop() {
        while (hasNextToken() && !activeSubscriptions.isEmpty()) {
            T token = nextToken();
            StreamSubscription subscription = activeSubscriptions.poll();
            Subscriber subscriber = subscription.subscriber;
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

    private void storeSubscription(StreamSubscription subscription) {
        if (!subscription.isCancelled()) {
            if (subscription.requested > 0) {
                activeSubscriptions.offer(subscription);
            } else {
                passiveSubscriptions.offer(subscription);
            }     
        }
    }

    private void completeSubscriptions(LinkedQueue<StreamSubscription> subscriptions) {
        for (;;) {
            StreamSubscription subscription = subscriptions.poll();
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

    public class StreamSubscription extends Link<StreamSubscription> implements Subscription {
        protected long requested = 0;
        private Subscriber subscriber;
        private volatile boolean lazyMode = false;

        public StreamSubscription(Subscriber subscriber) {
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
                subscriber = null;
                makeCompleted();
            } finally {
                locker.unlock();
            }
        }

        private void makeCompleted() {
            completed = true;
            unlink();
        }

        private void complete() {
            Subscriber subscriberLoc = subscriber;
            makeCompleted();
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

    static class Scalar2StreamSubscriber<T> implements Subscriber<T> {
        private ScalarSubscriber scalarSubscriber;
        private Subscription subscription;

        public Scalar2StreamSubscriber(ScalarSubscriber<? super T> s) {
            scalarSubscriber = s;
        }

        @Override
        public void onSubscribe(Subscription s) {
            subscription = s;
            s.request(1);
        }

        @Override
        public void onNext(T t) {
            scalarSubscriber.onComplete(t);
            subscription.cancel();
        }

        @Override
        public void onError(Throwable t) {
            scalarSubscriber.onError(t);
            subscription.cancel();
        }

        @Override
        public void onComplete() {
            scalarSubscriber.onComplete(null);
            subscription.cancel();
        }
    }
}
