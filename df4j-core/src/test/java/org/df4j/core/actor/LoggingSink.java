package org.df4j.core.actor;

import org.df4j.core.asyncproc.ScalarResult;

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;

public class LoggingSink implements Flow.Subscriber<Long> {
    private final ScalarResult<Long> asyncResult = new ScalarResult<>();
    Logger parent;
    final String name;
    AtomicInteger received = new AtomicInteger(0);
    boolean completed = false;

    public LoggingSink(Logger parent, int maxNumber, String name) {
        this.parent = parent;
        this.name = name;
        parent.registerAsyncResult(asyncResult);
    }

    @Override
    public void onSubscribe(Flow.Subscription s) {
        s.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(Long t) {
        if (t<0) {
            throw new IllegalArgumentException();
        }
        parent.println(name+": onNext "+t);
        received.incrementAndGet();
    }

    @Override
    public void onComplete() {
        parent.println(name+": onComplete after "+received.get()+" onNext");
        this.completed = true;
        asyncResult.onComplete();
    }

    @Override
    public void onError(Throwable t) {
        parent.println(name+": onError after "+received.get()+" onNext");
        this.completed = true;
        asyncResult.onError(t);
    }

    @Override
    public String toString() {
        return "LoggingSink "+name+", received:"+received.get()+ ", completed:"+completed;
    }
}
