package org.df4j.core.async;

import org.df4j.core.Port;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Promise is an asynchronous counterpart of {@link Future}.
 * While  {@link Future} passes information from an asynchronous computation
 * to a synchronous computation (thread),
 * {@link Promise)  passes information from an asynchronous computation to
 * another asynchronous computation.
 *
 * @param <T>
 */
public interface Promise<T> {
    void postTo(Port<T> request);

    default Future<T> toFuture() {
        CompletableFuture<T> f = new CompletableFuture<>();
        postTo(m->f.complete(m));
        return f;
    }

    static <T> Promise<T> completedPromise(T value) {
        return new Promise<T>() {
            @Override
            public void postTo(Port<T> request) {
                request.post(value);
            }
        };
    }
}
