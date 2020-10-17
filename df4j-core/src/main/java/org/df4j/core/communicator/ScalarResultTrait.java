package org.df4j.core.communicator;

import org.df4j.protocol.Scalar;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.concurrent.*;

/**
 * {@link ScalarResultTrait} can be considered as a one-shot multicast {@link AsyncArrayBlockingQueue}:
 *   once set, it always satisfies {@link ScalarResultTrait#subscribe(Scalar.Observer)}
 * <p>
 * Universal standalone connector for scalar values.
 * Has synchronous (Future), and asynchronous (both for scalar and stream kinds of subscribers)
 * interfaces on output end.
 *  Similar to {@link CompletableFuture}&lt;{@link R}&gt;
 *
 * @param <R> the type of completion value
 */
public interface ScalarResultTrait<R> extends CompletionI, Scalar.Source<R>, Future<R> {
    R getResult();
    void setResult(R result);
    LinkedList<CompletionSubscription> getSubscriptions();
    Throwable getCompletionException();

    @Override
    default void subscribe(Scalar.Observer<? super R> subscriber) {
        synchronized(this) {
            if (!isCancelled() && getSubscriptions() != null) {
                ValueSubscription subscription = new ValueSubscription(this, subscriber);
                getSubscriptions().add(subscription);
                subscriber.onSubscribe(subscription);
                return;
            }
        }
        Throwable completionException = getCompletionException();
        if (completionException == null) {
            subscriber.onSuccess(getResult());
        } else {
            subscriber.onError(completionException);
        }
    }

    @Override
    default boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    default boolean isCancelled() {
        return false;
    }

    @Override
    default boolean isDone() {
        return isCompleted();
    }

    @Override
    default R get() throws InterruptedException {
        blockingAwait();
        return getResult();
    }

    @Override
    default R get(long timeout, @NotNull TimeUnit unit) throws TimeoutException {
        if (blockingAwait(timeout, unit)) {
            return getResult();
        } else {
            throw new TimeoutException();
        }
    }

    class ValueSubscription<R> extends CompletionSubscription {
        ScalarResultTrait<R> scalarResultTrait;

        public ValueSubscription(ScalarResultTrait<R> completion, Scalar.Observer<? super R> subscriber) {
            super((Completion) completion, subscriber);
            this.scalarResultTrait = completion;
        }

        void onComplete() {
            Throwable completionException = scalarResultTrait.getCompletionException();
            if (completionException == null) {
                ((Scalar.Observer<? super R>)subscriber).onSuccess(scalarResultTrait.getResult());
            } else {
                subscriber.onError(completionException);
            }
        }
    }
}
