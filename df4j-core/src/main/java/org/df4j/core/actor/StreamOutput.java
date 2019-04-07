package org.df4j.core.actor;

import org.df4j.core.asyncproc.AsyncProc;
import org.df4j.core.asyncproc.Semafor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.HashSet;

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
 *  Though it extends {@link Actor}, it is a bounded connector and not an independent node.
 */
public class StreamOutput<T> extends Actor implements Publisher<T> {
    /**
     * unblocked when buffer has room for at least 1 token
     */
    protected final int capacity;
    /** buffer for imput tokens */
    protected final StreamInput<T> tokens;
    /** subscription which requested more tokens  */
    protected final StreamInput<ReactiveSubscription> activeSubscriptions = new StreamInput<>(this);
    protected final HashSet<ReactiveSubscription> passiveSubscriptions = new HashSet<>();
    protected boolean completed = false;

    public StreamOutput(AsyncProc outerActor, int capacity) {
        this.capacity = capacity;
        tokens = new StreamInput<>(this, capacity);
        tokens.setRoomLockIn(outerActor);
        super.setExecutor(directExec);
        start();
    }

    public StreamOutput(AsyncProc actor) {
        this(actor, 16);
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        ReactiveSubscription newSubscription = new ReactiveSubscription(subscriber);
        synchronized(this){
            passiveSubscriptions.add(newSubscription);
        }
        subscriber.onSubscribe(newSubscription);
    }

    public synchronized boolean isCompleted() {
        return completed;
    }

    public synchronized void onNext(T item) {
        if (tokens.size() >= capacity) {
            throw new IllegalStateException("buffer overflow");
        }
        tokens.onNext(item);
    }

    public synchronized void onComplete() {
        if (completed) {
            return; // completed already
        }
        completed = true;
        tokens.onComplete();
        for (ReactiveSubscription subs: passiveSubscriptions) {
            subs.onComplete();
        }
    }

    public synchronized void onError(Throwable throwable) {
        if (completed) {
            return; // completed already
        }
        completed = true;
        tokens.onError(throwable);
        for (ReactiveSubscription subs: passiveSubscriptions) {
            subs.onError(throwable);
        }
    }

    /** matches tokens and subscriptions
     */
    @Override
    protected void runAction() {
        ReactiveSubscription subscription = activeSubscriptions.current();
        T token = tokens.current();
        Subscriber<? super T> subscriber = subscription.subscriber;
        if (token != null) {
            if (subscription.isCancelled()) {
                throw new IllegalStateException("subscribtion cancelled");
            }
            subscription.decRequest();
            System.out.println(" runAction post "+token);
            subscriber.onNext(token);
        } else { // completed
            if (subscription.isCancelled()) {
                return;
            }
            System.out.println(" runAction complete "+subscriber);
            Throwable completionException = tokens.getCompletionException();
            if (completionException == null) {
                subscriber.onComplete();
            } else {
                subscriber.onError(completionException);
            }
        }
    }

    class ReactiveSubscription implements Subscription {
        protected volatile Subscriber<? super T> subscriber;
        private long requested = 0;

        public ReactiveSubscription(Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
        }

        public boolean isCancelled() {
            return subscriber == null;
        }

        public synchronized void decRequest() {
            requested--;
            if (requested == 0) {
                passiveSubscriptions.add(this);
            } else {
                activeSubscriptions.onNext(this);
            }
        }

        @Override
        public  void request(long n) {
            if (n <= 0){
                subscriber.onError(new IllegalArgumentException());
                return;
            }
            synchronized(this) {
                if (isCancelled()) {
                    return;
                }
                boolean wasPassive = requested == 0;
                requested += n;
                if (wasPassive) {
                    passiveSubscriptions.remove(this);
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

        public synchronized void onComplete() {
            Subscriber<? super T> subs = subscriber;
            cancel();
            subs.onComplete();
        }

        public synchronized void onError(Throwable th) {
            Subscriber<? super T> subs = subscriber;
            cancel();
            subs.onError(th);
        }
    }
}
