package org.df4j.core.node.messagescalar;

import org.df4j.core.connector.messagescalar.ScalarPublisher;
import org.df4j.core.connector.messagescalar.ScalarSubscriber;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * an unblocking single-shot output parameter
 *
 * @param <R>
 */
public class CompletablePromise<R> implements ScalarPublisher<R> {
    /** place for demands */
    private Queue<ScalarSubscriber<? super R>> requests = new ArrayDeque<>();
    protected boolean completed = false;
    protected R result = null;
    protected Throwable exception;

    @Override
    public synchronized <S extends ScalarSubscriber<? super R>> S subscribe(S subscriber) {
        if (completed) {
            subscriber.post(result);
        } else if (exception != null) {
            subscriber.postFailure(exception);
        } else {
            requests.add(subscriber);
        }
        return subscriber;
    }

    public synchronized void complete(R result) {
        this.result = result;
        this.completed = true;
        for (ScalarSubscriber<? super R> subscriber: requests) {
            subscriber.post(result);
        }
        requests = null;
    }

    public synchronized void completeExceptionally(Throwable exception) {
        this.exception = exception;
        for (ScalarSubscriber<? super R> subscriber: requests) {
            subscriber.postFailure(exception);
        }
        requests = null;
    }

    public boolean isDone() {
        return completed || (exception != null);
    }

    public boolean isCompletedExceptionally() {
        return exception != null;
    }

    public synchronized int getNumberOfSubscribers() {
        return requests.size();
    }

    @Override
    public String toString() {
        int count = getNumberOfSubscribers();
        StringBuilder sb = new StringBuilder();
        if (completed) {
            sb.append("[Completed normally]");
        } else if (exception != null) {
            sb.append("[Completed exceptionally]");
        } else if (count == 0) {
            sb.append("[Not completed]");
        } else {
            sb.append("[Not completed, ");
            sb.append(count);
            sb.append(" dependents]");
        }
        return sb.toString();
    }
}
