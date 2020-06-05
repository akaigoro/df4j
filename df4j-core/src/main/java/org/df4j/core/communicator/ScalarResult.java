package org.df4j.core.communicator;

import org.df4j.protocol.Completable;
import org.df4j.protocol.Scalar;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * {@link ScalarResult} can be considered as a one-shot multicast {@link AsyncArrayBlockingQueue}:
 *   once set, it always satisfies {@link ScalarResult#subscribe(Scalar.Observer)}
 * <p>
 * Universal standalone connector for scalar values.
 * Has synchronous (Future), and asynchronous (both for scalar and stream kinds of subscribers)
 * interfaces on output end.
 *  Similar to {@link CompletableFuture}&lt;{@link R}&gt;
 *
 * @param <R> the type of completion value
 */
public class ScalarResult<R> extends Completion implements ScalarResultTrait<R> {
    private R result;

    @Override
    public R getResult() {
        return result;
    }

    @Override
    public void setResult(R result) {
        this.result = result;
    }
}
