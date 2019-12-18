package org.df4j.core.communicator;

import org.df4j.core.protocol.Signal;

import java.util.LinkedList;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

public abstract class SignalSubscribers<S extends Signal.Subscriber> {
    protected LinkedList<S> subscribers;
    protected boolean completed;

    public synchronized boolean isCompleted() {
        return completed;
    }

    protected LinkedList<S> getSubscribers() {
        if (subscribers == null) {
            subscribers = new LinkedList<>();
        }
        return subscribers;
    }

    public void subscribe(S subscriber) {
        synchronized(this) {
            if (!completed) {
                getSubscribers().add(subscriber);
                return;
            }
        }
        subscriber.onComplete();
    }

    public synchronized boolean unsubscribe(S co) {
        return subscribers.remove(co);
    }

    protected LinkedList<S> removeSubscribers() {
        LinkedList<S> res = subscribers;
        subscribers = null;
        return res;
    }

    public void onComplete() {
        LinkedList<S> subs;
        synchronized(this) {
            if (completed) {
                return;
            }
            completed = true;
            notifyAll();
            if (subscribers == null) {
                return;
            }
            subs = subscribers;
            subscribers = null;
        }
        for (;;) {
            S sub = subs.poll();
            if (sub == null) {
                break;
            }
            sub.onComplete();
        }
    }


    public S poll() {
        return subscribers.poll();
    }

    public synchronized void blockingAwait()  {
        while (!completed) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new CompletionException(e);
            }
        }
    }

    public synchronized boolean blockingAwait(long timeout) {
        long targetTime = System.currentTimeMillis()+timeout;
        for (;;) {
            if (completed) {
                return true;
            }
            if (timeout <= 0) {
                return false;
            }
            try {
                wait(timeout);
            } catch (InterruptedException e) {
                throw new CompletionException(e);
            }
            timeout = targetTime - System.currentTimeMillis();
        }
    }

    public synchronized boolean blockingAwait(long timeout, TimeUnit unit) {
        return blockingAwait(unit.toMillis(timeout));
    }
}
