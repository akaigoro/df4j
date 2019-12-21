package org.df4j.core.communicator;

import org.df4j.protocol.Completion;

import java.util.LinkedList;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Completable implements Completion.CompletableObserver, Completion.CompletableSource {
    private final Lock bblock = new ReentrantLock();
    private final Condition completedCond = bblock.newCondition();
    protected Throwable completionException;
    protected LinkedList<Completion.CompletableObserver> subscribers;
    protected boolean completed;

    public Throwable getCompletionException() {
        return completionException;
    }

    public void setCompletionException(Throwable completionException) {
        this.completionException = completionException;
    }

    public void subscribe(Completion.CompletableObserver co) {
        bblock.lock();
        try {
            if (!completed) {
                LinkedList<Completion.CompletableObserver> subscribers = getSubscribers();
                subscribers.add(co);
                return;
            }
        } finally {
            bblock.unlock();
        }
        if (getCompletionException() == null) {
            co.onComplete();
        } else {
            Throwable completionException = getCompletionException();
            co.onError(completionException);
        }
    }

    public void onError(Throwable e) {
        LinkedList<Completion.CompletableObserver> subs;
        bblock.lock();
        try {
            if (completed) {
                return;
            }
            completed = true;
            setCompletionException(e);
            completedCond.signalAll();
            if (subscribers == null) {
                return;
            }
            subs = subscribers;
            subscribers = null;
        } finally {
            bblock.unlock();
        }
        for (;;) {
            Completion.CompletableObserver sub = subs.poll();
            if (sub == null) {
                break;
            }
            sub.onError(e);
        }
    }

    public synchronized void blockingAwait() {
        bblock.lock();
        try {
            while (!completed) {
                try {
                    completedCond.await();
                } catch (InterruptedException e) {
                    throw new CompletionException(e);
                }
            }
        } finally {
            bblock.unlock();
        }
        if (completionException != null) {
            throw new CompletionException(completionException);
        }
    }

    public boolean blockingAwait(long timeout) {
        boolean result;
        bblock.lock();
        try {
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
                    completedCond.await(timeout, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    throw new CompletionException(e);
                }
                timeout = targetTime - System.currentTimeMillis();
            }
        } finally {
            bblock.unlock();
        }
        if (!result) {
            return false;
        }
        if (completionException != null) {
            throw new CompletionException(completionException);
        }
        return true;
    }

    public boolean blockingAwait(long timeout, TimeUnit unit) {
        bblock.lock();
        try {
            boolean result = blockingAwait(unit.toMillis(timeout));
            if (!result) {
                return false;
            }
            if (completionException != null) {
                throw new CompletionException(completionException);
            }
            return true;
        } finally {
            bblock.unlock();
        }
    }

    public boolean isCompleted() {
        bblock.lock();
        try {
            return completed;
        } finally {
            bblock.unlock();
        }
    }

    protected LinkedList<Completion.CompletableObserver> getSubscribers() {
        bblock.lock();
        try {
            if (subscribers == null) {
                subscribers = new LinkedList<>();
            }
            return subscribers;
        } finally {
            bblock.unlock();
        }
    }

    public boolean unsubscribe(Completion.CompletableObserver co) {
        bblock.lock();
        try {
            return subscribers.remove(co);
        } finally {
            bblock.unlock();
        }
    }

    public void onComplete() {
        LinkedList<Completion.CompletableObserver> subs;
        bblock.lock();
        try {
        } finally {
            bblock.unlock();
        }
        bblock.lock();
        try {
            if (completed) {
                return;
            }
            completed = true;
            completedCond.signalAll();
            if (subscribers == null) {
                return;
            }
            subs = subscribers;
            subscribers = null;
        } finally {
            bblock.unlock();
        }
        for (;;) {
            Completion.CompletableObserver sub = subs.poll();
            if (sub == null) {
                break;
            }
            sub.onComplete();
        }
    }

    public Completion.CompletableObserver poll() {
        bblock.lock();
        try {
            return subscribers.poll();
        } finally {
            bblock.unlock();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        LinkedList<Completion.CompletableObserver> subscribers = this.subscribers;
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
