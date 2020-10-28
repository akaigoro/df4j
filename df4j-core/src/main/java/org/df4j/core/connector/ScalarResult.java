package org.df4j.core.connector;

import org.df4j.protocol.Scalar;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * {@link ScalarResult} can be considered as a one-shot multicast {@link AsyncArrayBlockingQueue}:
 *   once set, it always satisfies {@link ScalarResult#subscribe(Scalar.Subscriber)}
 * <p>
 * Universal standalone connector for scalar values.
 * Has synchronous (Future), and asynchronous (both for scalar and stream kinds of subscribers)
 * interfaces on output end.
 *  Similar to {@link CompletableFuture}&lt;{@link R}&gt;
 *
 * @param <R> the type of completion value
 */
public class ScalarResult<R> extends Completion implements Scalar.Source<R>, Scalar.Publisher<R> {
    private R result;

    /**
     * 	complete(T value)
     * If not already completed, sets the value returned by get() and related methods to the given value.
     * @param result the value returned by get()
     */
    public synchronized void setResult(R result) {
        if (isCompleted()) {
            return;
        }
        this.result = result;
        complete();
    }

    @Override
    public void subscribe(Scalar.Subscriber<? super R> subscriber) {
        synchronized(this) {
            if (!isCompleted() && getSubscriptions() != null) {
                ValueSubscription subscription = new ValueSubscription(subscriber);
                getSubscriptions().add(subscription);
                subscriber.onSubscribe(subscription);
                return;
            }
        }
        Throwable completionException = getCompletionException();
        if (completionException == null) {
            subscriber.onSuccess(result);
        } else {
            subscriber.onError(completionException);
        }
    }

    @Override
    public R get() throws InterruptedException {
        await();
        return result;
    }

    @Override
    public R get(long timeout, @NotNull TimeUnit unit) throws TimeoutException, InterruptedException {
        if (await(timeout, unit)) {
            return result;
        } else {
            throw new TimeoutException();
        }
    }

    class ValueSubscription extends CompletionSubscription {

        public ValueSubscription(Scalar.Subscriber<? super R> subscriber) {
            super(ScalarResult.this, subscriber);
        }

        void onComplete() {
            Throwable completionException = getCompletionException();
            if (completionException == null) {
                ((Scalar.Subscriber<? super R>)subscriber).onSuccess(result);
            } else {
                subscriber.onError(completionException);
            }
        }
    }
}
