package org.df4j.core.simplenode.messagescalar;

import org.df4j.core.boundconnector.messagescalar.AsyncResult;
import org.df4j.core.boundconnector.messagescalar.ConstInput;
import org.df4j.core.boundconnector.messagescalar.ScalarPublisher;
import org.df4j.core.boundconnector.messagescalar.ScalarSubscriber;
import org.df4j.core.tasknode.AsyncProc;
import org.df4j.core.tasknode.messagescalar.AsyncFunction;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.*;

/**
 *
 * @param <R> type of input parameter
 * @param <R> type of result
 */
public class CompletablePromise<R> implements AsyncResult<R> {

    /** place for demands */
    private ScalarSubscriber<? super R> subscriber = null;
    protected boolean done = false;
    protected R result = null;
    protected Throwable exception;
    protected final AsyncProc asyncProc;

    public CompletablePromise(AsyncProc asyncProc) {
        this.asyncProc = asyncProc;
    }

    public CompletablePromise() {
        this.asyncProc = null;
    }

    @Override
    public synchronized <S extends ScalarSubscriber<? super R>> S subscribe(S subscriber) {
        if (done) {
            subscriber.complete(result);
        } else if (exception != null) {
            subscriber.completeExceptionally(exception);
        } else if (this.subscriber == null) {
            this.subscriber = subscriber;
        } else if (this.subscriber instanceof Lobby){
            ((Lobby) this.subscriber).subscribe(subscriber);
        } else {
            ScalarSubscriber<? super R> old = this.subscriber;
            Lobby<R> lobby = new Lobby<R>();
            lobby.subscribe(old);
            lobby.subscribe(subscriber);
            this.subscriber = lobby;
        }
        return subscriber;
    }

    public synchronized boolean complete(R result) {
        if (isDone()) {
            return false;
        }
        this.result = result;
        this.done = true;
        notifyAll();
        if (subscriber != null) {
            subscriber.complete(result);
            subscriber = null;
        }
        return true;
    }

    public synchronized boolean completeExceptionally(Throwable exception) {
        if (exception == null) {
            throw new IllegalArgumentException("AsyncResult::completeExceptionally(): argument may not be null");
        }
        if (isDone()) {
            return false;
        }
        this.exception = exception;
        subscriber.completeExceptionally(exception);
        subscriber = null;
        return true;
    }

    /**
     * wrong API design. Generally, Future is not a task and cannot be cancelled.
     * @param mayInterruptIfRunning
     * @return
     */
    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    public boolean isDone() {
        return done;
    }

    @Override
    public synchronized R get() throws InterruptedException, ExecutionException {
        for (;;) {
            if (result != null) {
                return result;
            } else if (exception != null) {
                return throwStoredException();
            } else {
                wait();
            }
        }
    }

    private R throwStoredException() throws ExecutionException {
        Throwable x=exception, cause;
        if (x instanceof CancellationException)
            throw (CancellationException)x;
        if ((x instanceof CompletionException) &&
                (cause = x.getCause()) != null)
            x = cause;
        throw new ExecutionException(x);
    }

    @Override
    public synchronized R get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        long end = System.currentTimeMillis()+ unit.toMillis(timeout);
        for (;;) {
            if (done) {
                return result;
            } else if (exception != null) {
                throwStoredException();
            } else {
                long timeout1 = end - System.currentTimeMillis();
                if (timeout1 <= 0) {
                    throw new TimeoutException();
                }
                wait(timeout1);
            }
        }
    }

    final class Lobby<R> extends AsyncProc implements ScalarPublisher<R>, ScalarSubscriber<R> {
        private Queue<ScalarSubscriber<? super R>> requests = new ArrayDeque<>();
        private ConstInput<R> input = new ConstInput<>(this);

        public Executor getExecutor() {
            if (asyncProc != null) {
                Executor exec = asyncProc.getExecutor();
                if (exec != null) {
                    return exec;
                }
            }
            return ForkJoinPool.commonPool();
        }

        @Override
        protected boolean isStarted() {
            return false;
        }

        @Override
        public <S extends ScalarSubscriber<? super R>> S subscribe(S subscriber) {
            requests.add(subscriber);
            return subscriber;
        }

        @Override
        public void run() {
            R value = input.getValue();
            for (ScalarSubscriber<? super R> request: requests) {
                request.complete(value);
            }
        }

        @Override
        public boolean complete(R message) {
            return input.complete(message);
        }
    }
}
