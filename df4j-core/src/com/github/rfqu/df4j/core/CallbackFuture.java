/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.core;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A kind of dataflow variable: single input, multiple asynchronous outputs.
 * Connects Actors and Threads.
 * Actors are allowed to send messages to it, but not to get from. 
 * Threads are allowed both to send and get.
 *
 * @param <T> the type of accepted messages
 */
public class CallbackFuture<T> implements Callback<T>, Future<T> {
	protected volatile boolean _hasValue;
    protected volatile T value;
    protected volatile Throwable exc;
    
    public CallbackFuture(){
    }
    
    public CallbackFuture(ResultSource<T> source){
        source.addListener(this);
    }
    
    public CallbackFuture<T> listenTo(ResultSource<T> source){
        source.addListener(this);
        return this;        
    }
    
    @Override
    public synchronized void send(T message) {
        if (_hasValue) {
            throw new IllegalStateException("has value already");
        }
        this.value=message;
        _hasValue=true;
        notifyAll();
    }

    @Override
    public synchronized void sendFailure(Throwable exc) {
        if (_hasValue) {
            throw new IllegalStateException("has value already");
        }
        this.exc=exc;
        _hasValue=true;
        notifyAll();
    }
    
    @Override
    public boolean isDone() {
        return _hasValue;
    }

    public Throwable getException() {
        return exc;
    }

    /**
     * waits until a message arrive
     * @return received message
     * @throws InterruptedException if the current thread was interrupted while waiting
     */
    @Override
    public synchronized T get() throws InterruptedException, ExecutionException {
        while (!_hasValue) {
            wait();
        }
        if (exc==null) {
            return value;
        } else {
            throw new ExecutionException(exc);
        }
    }

    @Override
    public synchronized T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (value!=null) {
            return value;
        }
        long duration = unit.toMillis(timeout);
        long startTime=System.currentTimeMillis();
        long endTime=startTime+duration;
        for (;;) {
            wait(duration);            
            if (value!=null) {
                return value;
            }
            long currentTime=System.currentTimeMillis();
            duration=(endTime-currentTime);
            if (duration<=0) {
                throw new TimeoutException();
            }
        }
    }

    public T get(long timeoutMillis) throws InterruptedException, ExecutionException, TimeoutException {
        return get(timeoutMillis, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isCancelled() {
        // TODO Auto-generated method stub
        return false;
    }

    public static <R> R getFrom(ResultSource<R> source) throws InterruptedException, ExecutionException {
        return new CallbackFuture<R>(source).get();
    }

}
