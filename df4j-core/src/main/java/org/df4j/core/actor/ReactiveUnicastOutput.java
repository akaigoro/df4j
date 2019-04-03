package org.df4j.core.actor;

import org.df4j.core.Port;
import org.df4j.core.actor.ext.StreamInput;
import org.df4j.core.asynchproc.Semafor;
import org.df4j.core.asynchproc.AsyncProc;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Non-blocking analogue of blocking queue.
 * Serves multiple consumers (subscribers)
 * demonstrates usage of class {@link Semafor} for handling back pressure?
 *
 * Each message is routed to exactly one subscriber. When no one subscriber exists, blocks.
 * Has limited buffer for messages.
 *
 * @param <T> the type of transferred messages
 *
 *  Though it extends {@link Actor}, it is a connector and not an independent node.
 */
public class ReactiveUnicastOutput<T> extends Actor implements Port<T>, Publisher<T> {
    protected final AsyncProc.Lock outerLock;
    protected final int capacity;
    protected StreamInput<T> buffer = new StreamInput<>(this);
    protected StreamInput<UnicastReactiveSubscription> activeSubscriptions = new StreamInput<>(this);
    protected boolean completed = false;
    protected Throwable completionException = null;

    public ReactiveUnicastOutput(AsyncProc outerActor, int capacity) {
        outerLock = outerActor.new Lock(true);
        this.capacity = capacity;
    }

    public ReactiveUnicastOutput(AsyncProc actor) {
        this(actor, 16);
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        UnicastReactiveSubscription newSubscription = new UnicastReactiveSubscription(subscriber);
        subscriber.onSubscribe(newSubscription);
    }

    public synchronized boolean isCompleted() {
        return completed;
    }

    public synchronized void onNext(T item) {
        if (buffer.size() >= capacity) {
            throw new IllegalStateException("buffer overflow");
        }
        buffer.onNext(item);
        if (buffer.size() >= capacity) {
            outerLock.block();
        }
    }

    public synchronized void onComplete() {
        if (completed) {
            return; // completed already
        }
        completed = true;
        outerLock.block();
    }

    @Override
    public synchronized void onError(Throwable throwable) {
        completionException = throwable;
        onComplete();
    }

    @Override
    protected void runAction() {
        UnicastReactiveSubscription subscription = activeSubscriptions.current();
        Subscriber<? super T> subscriber = subscription.subscriber;
        if (!completed) {
            if (subscription.isCancelled()) {
                throw new IllegalStateException("subscribtion cancelled");
            }
            subscription.decRequest();
            T token = buffer.current();
            subscriber.onNext(token);
        } else {
            if (subscription.isCancelled()) {
                return;
            }
            subscription.cancel();
            if (completionException == null) {
                subscriber.onComplete();
            } else {
                subscriber.onError(completionException);
            }
        }
    }

    class UnicastReactiveSubscription implements Subscription {
        protected volatile Subscriber<? super T> subscriber;
        private long requested = 0;

        public UnicastReactiveSubscription(Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
        }

        public boolean isCancelled() {
            return subscriber == null;
        }

        public void decRequest() {
            if (isCancelled()) {
                throw new IllegalStateException("subscriptopn cancelled");
            }
            synchronized(this) {
                requested --;
                if (requested != 0) {
                    activeSubscriptions.onNext(this);
                }
            }
        }

        @Override
        public  void request(long n) {
            if (n <= 0){
                subscriber.onError(new IllegalArgumentException());
                return;
            }
            if (isCancelled()) {
                return;
            }
            synchronized(this) {
                boolean wasPassive = requested == 0;
                requested += n;
                if (wasPassive) {
                    activeSubscriptions.onNext(this);
                }
            }
        }

        /**
         * subscription closed by request of subscriber
         */
        public synchronized void cancel() {
            if (isCancelled()) {
                return;
            }
            subscriber = null;
        }
    }
}
