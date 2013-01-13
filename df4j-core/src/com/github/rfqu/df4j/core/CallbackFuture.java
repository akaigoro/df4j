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

import java.util.concurrent.CancellationException;
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
    
    public CallbackFuture() {
    }
    
    public CallbackFuture(Promise<T> source) {
        source.addListener(this);
    }
    
    /** connects to an EventSource source.
     * The source is supposed to invoke send(T message) on this instance.
     * @param source
     */
    public CallbackFuture<T> listenTo(Promise<T> source) {
        source.addListener(this);
        return this;        
    }
    
    /** sends a message to this instance and then to its listeners.
     */
    @Override
    public synchronized void post(T message) {
        if (_hasValue) {
            throw new IllegalStateException("has value already");
        }
        this.value=message;
        _hasValue=true;
        notifyAll();
    }

    @Override
    public synchronized void postFailure(Throwable exc) {
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

    /**
     * @return null if was interrupted
     */
    public Throwable getException() {
        return exc;
    }

    /**
     * waits until a message arrive
     * @return received message
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws ExecutionException if failure was sent.
     * @throws CancellationException if this Future was cancelled
     */
    @Override
    public synchronized T get() throws InterruptedException, ExecutionException {
        while (!_hasValue) {
            wait();
        }
        if (value!=null) {
            return value;
        } else if (exc!=null){
            throw new ExecutionException(exc);
        } else {
            throw new CancellationException();
        }
    }

    /** waits until a message arrive, with timeout.
     * 
     * @param timeout timeout in units
     * @param unit units of time to measure timeout
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws TimeoutException if timeout occur
     * @throws ExecutionException if failure was sent.
     * @throws CancellationException if this Future was cancelled
     */
    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return get(unit.toMillis(timeout));
    }

    /** waits until a message arrive, with timeout.
     * 
     * @param timeoutMillis timeout in milliseconds
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws TimeoutException if timeout occur
     * @throws ExecutionException if failure was sent.
     * @throws CancellationException if this Future was cancelled
     */
    public synchronized T get(long timeoutMillis) throws InterruptedException, ExecutionException, TimeoutException {
        long endTime=System.currentTimeMillis()+timeoutMillis;
        while (!_hasValue) {
            long duration=(endTime-System.currentTimeMillis());
            if (duration<=0) {
                throw new TimeoutException();
            }
            wait(duration);            
        }
        return get();
    }
    
    /**
     * Since this Future does not represent any task, 
     * this method is used to interrupt waiting threads.
     */
    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        if (_hasValue) {
            throw new IllegalStateException("has value already");
        }
        _hasValue=true;
        notifyAll();
        return true;
    }

    @Override
    public synchronized boolean isCancelled() {
        return _hasValue && value==null && exc==null;
    }

    public static <R> R getFrom(Promise<R> source) throws InterruptedException, ExecutionException {
        return new CallbackFuture<R>(source).get();
    }

}
