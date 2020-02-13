package org.df4j.core.communicator;

import org.df4j.protocol.Completable;
import org.df4j.protocol.Scalar;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.concurrent.*;

/**
 * {@link ScalarResult} can be considered as a one-shot multicast {@link AsyncArrayBlockingQueue}:
 *   once set, it always satisfies {@link ScalarResult#subscribe(Scalar.Observer)}
 * <p>
 * Universal standalone connector for scalar values.
 * Has synchronous (Future), and asynchronous (both for scalar and stream kinds of subscribers)
 * interfaces on output end.
 *  an equivalent to {@link CompletableFuture}&lt;{@link R}&gt;
 *
 * @param <R> the type of completion value
 */
public class ScalarResult<R> extends Completion implements Scalar.Source<R>, Future<R> {
    private R result;

    @Override
    public void subscribe(Scalar.Observer<? super R> subscriber) {
        bblock.lock();
        try {
            if (!isCancelled() && subscriptions != null) {
                ValueSubscription subscription = new ValueSubscription(subscriber);
                subscriptions.add(subscription);
                subscriber.onSubscribe(subscription);
                return;
            }
        } finally {
            bblock.unlock();
        }
        Throwable completionException = getCompletionException();
        if (completionException == null) {
            subscriber.onSuccess(result);
        } else {
            subscriber.onError(completionException);
        }
    }

    @Override
    protected void setResult(Object result) {
        this.result = (R) result;
    }

    /**
     * completes this {@link ScalarResult} with value
     * @param message completion value
     */
    public void onSuccess(R message) {
        _onComplete(message, null);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return isCompleted();
    }

    @Override
    public R get() throws InterruptedException {
        join();
        return result;
    }

    @Override
    public R get(long timeout, @NotNull TimeUnit unit) throws TimeoutException {
        if (super.blockingAwait(timeout, unit)) {
            return result;
        } else {
            throw new TimeoutException();
        }
    }

    class ValueSubscription extends CompletionSubscription {

        protected ValueSubscription(Scalar.Observer<? super R> subscriber) {
            super(subscriber);
        }

        public void onComplete() {
            if (completionException == null) {
                ((Scalar.Observer<? super R>)subscriber).onSuccess(result);
            } else {
                subscriber.onError(completionException);
            }
        }
    }

    /**
     * Returns the result value when complete, or throws an
     * (unchecked) exception if completed exceptionally.
     *
     * @return the result value
     * @throws CompletionException if this  {@link ScalarResult} completed exceptionally
     */
 //   public R join() {
 //       return cf.join();
  //  }
}
