package org.df4j.core.tasknode.messagescalar;

import org.df4j.core.boundconnector.messagescalar.AsyncResult;
import org.df4j.core.boundconnector.messagescalar.ScalarSubscriber;
import org.df4j.core.tasknode.AsyncProc;

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
    private Lobby<R> requests = new Lobby<>();
    protected boolean completed = false;
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
        if (completed) {
            subscriber.post(result);
        } else if (exception != null) {
            subscriber.postFailure(exception);
        } else {
            requests.subscribe(subscriber);
        }
        return subscriber;
    }

    public synchronized boolean complete(R result) {
        if (isDone()) {
            return false;
        }
        this.result = result;
        this.completed = true;
        notifyAll();
        requests.post(result);
        requests = null;
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
        requests.postFailure(exception);
        requests = null;
        return true;
    }

    /**
     * wrong API design. Future is not a task.
     * @param mayInterruptIfRunning
     * @return
     */
    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    public boolean isDone() {
        return completed || (exception != null);
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
            if (completed) {
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

    class Lobby<R> {
        private ScalarSubscriber<? super R> request;
        private Queue<ScalarSubscriber<? super R>> requests;

        public synchronized  <S extends ScalarSubscriber<? super R>> void subscribe(S subscriber) {
            if (request == null) {
                request = subscriber;
            } else {
                if (requests == null) {
                    requests = new ArrayDeque<>();
                }
                requests.add(subscriber);
            }
        }

        private Executor getExecutor() {
            if (asyncProc != null) {
                Executor exec = asyncProc.getExecutor();
                if (exec != null) {
                    return exec;
                }
            }
            return ForkJoinPool.commonPool();
        }

        void post(R result) {
            if (request == null) {
                return;
            }
            request.post(result);
            if (requests == null) {
                return;
            }
            getExecutor().execute(new PostResult<R>(requests, result));
        }

        void postFailure(Throwable exception) {
            if (request == null) {
                return;
            }
            request.postFailure(exception);
            if (requests == null) {
                return;
            }
            getExecutor().execute(new PostFailure(requests, exception));
        }
    }

    static class PostResult<R> implements Runnable {
        private final Queue<ScalarSubscriber<? super R>> requests;
        private final R result;

        PostResult(Queue<ScalarSubscriber<? super R>> requests, R result) {
            this.requests = requests;
            this.result = result;
        }

        @Override
        public void run() {
            for (ScalarSubscriber<? super R> subscriber: requests) {
                subscriber.post(result);
            }
        }
    }

    static class PostFailure<R> implements Runnable {
        private final Queue<ScalarSubscriber<? super R>> requests;
        private final Throwable exception;

        PostFailure(Queue<ScalarSubscriber<? super R>> requests, Throwable exception) {
            this.requests = requests;
            this.exception = exception;
        }

        @Override
        public void run() {
            for (ScalarSubscriber<? super R> subscriber: requests) {
                subscriber.postFailure(exception);
            }
        }
    }
}
