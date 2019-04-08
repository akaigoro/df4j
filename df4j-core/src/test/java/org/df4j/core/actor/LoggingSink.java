package org.df4j.core.actor;

import org.df4j.core.asyncproc.AsyncResult;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;

public class LoggingSink<T> implements Subscriber<T> {
    private final AsyncResult<T> asyncResult = new AsyncResult<T>();
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
    public void onSubscribe(Subscription s) {
        s.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(T t) {
        parent.println(name+": onNext "+t);
        received.incrementAndGet();
    }

    @Override
    public void onError(Throwable t) {
        parent.println(name+": onError after "+received.get()+" onNext");
        this.completed = true;
        asyncResult.onError(t);
    }

    @Override
    public void onComplete() {
        parent.println(name+": onComplete after "+received.get()+" onNext");
        this.completed = true;
        asyncResult.onComplete();
    }

    @Override
    public String toString() {
        return "LoggingSink "+name+", received:"+received.get()+ ", completed:"+completed;
    }
}
