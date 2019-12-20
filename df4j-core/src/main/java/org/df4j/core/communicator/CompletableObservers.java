package org.df4j.core.communicator;

import org.df4j.core.protocol.Completion;

import java.util.LinkedList;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

public abstract class CompletableObservers<S> {
    protected Throwable completionException;
    protected LinkedList<S> subscribers;
    protected boolean completed;

    public Throwable getCompletionException() {
        return completionException;
    }

    public void setCompletionException(Throwable completionException) {
        this.completionException = completionException;
    }

    public void subscribe(S co) {
        synchronized(this) {
            if (!completed) {
                LinkedList<S> subscribers = getSubscribers();
                subscribers.add(co);
                return;
            }
        }
        if (getCompletionException() == null) {
            doOnComplete(co);
        } else {
            Throwable completionException = getCompletionException();
            doOnError(co, completionException);
        }
    }

    protected abstract void doOnError(S co, Throwable completionException);

    protected abstract void doOnComplete(S co);

    public void onError(Throwable e) {
        LinkedList<S> subs;
        synchronized(this) {
            if (completed) {
                return;
            }
            completed = true;
            setCompletionException(e);
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
            doOnError(sub, e);
        }
    }

    public synchronized void blockingAwait() {
        synchronized (this) {
            while (!completed) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    throw new CompletionException(e);
                }
            }
        }
        if (completionException != null) {
            throw new CompletionException(completionException);
        }
    }

    public synchronized boolean blockingAwait(long timeout) {
        boolean result;
        synchronized (this) {
            long targetTime = System.currentTimeMillis()+ timeout;
            for (;;) {
                if (completed) {
                    result = true;
                    break;
                }
                if (timeout <= 0) {
                    result = false;
                    break;
                }
                try {
                    wait(timeout);
                } catch (InterruptedException e) {
                    throw new CompletionException(e);
                }
                timeout = targetTime - System.currentTimeMillis();
            }
        }
        if (!result) {
            return false;
        }
        if (completionException != null) {
            throw new CompletionException(completionException);
        }
        return true;
    }

    public synchronized boolean blockingAwait(long timeout, TimeUnit unit) {
        boolean result;
        synchronized (this) {
            result = blockingAwait(unit.toMillis(timeout));
        }
        if (!result) {
            return false;
        }
        if (completionException != null) {
            throw new CompletionException(completionException);
        }
        return true;
    }

    public synchronized boolean isCompleted() {
        return completed;
    }

    protected LinkedList<S> getSubscribers() {
        if (subscribers == null) {
            subscribers = new LinkedList<>();
        }
        return subscribers;
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
            doOnComplete(sub);
        }
    }

    public S poll() {
        return subscribers.poll();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        LinkedList<S> subscribers = this.subscribers;
        Throwable completionException = this.completionException;
        int size = 0;
        if (subscribers!=null) {
            size=subscribers.size();
        }
        if (!completed) {
            sb.append("not completed; subscribers: "+size);
        } else if (completionException == null) {
            sb.append("completed successfully");
        } else {
            sb.append("completed with exception: ");
            sb.append(completionException.toString());
        }
        return sb.toString();
    }
}
