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

import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 
 * Common base for {@link CompletableFuture} and {@link com.github.rfqu.df4j.core.Request},
 * which differ in relations with listeners.
 *
 * @param <R>  type of result
 * @param <R>  type of listeners
 * @param <R>  type of result
 */
public abstract class CompletableFutureBase<R, L>
  implements Callback<R>, Future<R>
{
    protected boolean _hasValue;
    protected R value;
    protected Throwable exc;
    protected Object listener;

    protected abstract void passResult(L listenerLoc);
    protected abstract void passFailure(L listenerLoc);

    /**
     * @return null if was interrupted
     */
    public synchronized Throwable getException() {
        return exc;
    }

    // ==================== Future implementation

    @Override
    public synchronized boolean isDone() {
        return _hasValue;
    }

    /**
     * waits until a message arrive
     * 
     * @return received message
     * @throws InterruptedException
     *             if the current thread was interrupted while waiting
     * @throws ExecutionException
     *             if failure was sent.
     * @throws CancellationException
     *             if this Future was cancelled
     */
    @Override
    public synchronized R get() throws InterruptedException, ExecutionException {
        while (!_hasValue) {
            wait();
        }
        if (exc != null) {
            if (exc instanceof CancellationException) {
                throw (CancellationException)exc;
            } else {
                throw new ExecutionException(exc);
            }
        } else {
            return value;
        }
    }

    /**
     * waits until a message arrive, with timeout.
     * 
     * @param timeout
     *            timeout in units
     * @param unit
     *            units of time to measure timeout
     * @throws InterruptedException
     *             if the current thread was interrupted while waiting
     * @throws TimeoutException
     *             if timeout occur
     * @throws ExecutionException
     *             if failure was sent.
     * @throws CancellationException
     *             if this Future was cancelled
     */
    @Override
    public R get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return get(unit.toMillis(timeout));
    }

    /**
     * waits until a message arrive, with timeout.
     * 
     * @param timeoutMillis
     *            timeout in milliseconds
     * @throws InterruptedException
     *             if the current thread was interrupted while waiting
     * @throws TimeoutException
     *             if timeout occur
     * @throws ExecutionException
     *             if failure was sent.
     * @throws CancellationException
     *             if this Future was cancelled
     */
    public synchronized R get(long timeoutMillis) throws InterruptedException, ExecutionException, TimeoutException {
        long endTime = System.currentTimeMillis() + timeoutMillis;
        while (!_hasValue) {
            long duration = (endTime - System.currentTimeMillis());
            if (duration <= 0) {
                throw new TimeoutException();
            }
            wait(duration);
        }
        return get();
    }

    /**
     * Posts CancellationException to listeners.
     * @return
     *   true if this future was not yet done
     *   false if this future was already done or cancelled.
     */
    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        try {
            postFailure(new CancellationException());
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    @Override
    public synchronized boolean isCancelled() {
        return exc instanceof CancellationException;
    }

    // ==================== Callback implementation

    /**
     * sends a message to this instance and then to its listeners.
     * @throws IllegalStateException
     *    if value or exception already have been posted
     */
    @SuppressWarnings("unchecked")
    @Override
    public void post(R m) {
        Object listenerLoc;
        synchronized(this) {
            listenerLoc = listener;
            if (setHasValue(null)) {
                return;
            }
            value = m;
        }
        if (listenerLoc == null) {
            return;
        }
        if (listenerLoc instanceof ArrayList) {
            ArrayList<L> listenersLoc=(ArrayList<L>) listenerLoc;
            for (int k=0; k<listenersLoc.size(); k++) {
                passResult(listenersLoc.get(k));
            }
        } else {
            passResult((L)listenerLoc);
        }
    }

    
    /**
     * @throws IllegalStateException
     *    if value or exception already have been posted
     */
    @SuppressWarnings("unchecked")
    @Override
    public void postFailure(Throwable newExc) {
        Object listenerLoc;
        synchronized(this) {
            listenerLoc = listener;
            if (setHasValue(newExc)) {
                return;
            }
            this.exc = newExc;
        }
        if (listenerLoc == null) {
            return;
        }
        if (listenerLoc instanceof ArrayList<?>) {
            ArrayList<L> listenersLoc=(ArrayList<L>) listenerLoc;
            for (int k=0; k<listenersLoc.size(); k++) {
                passFailure(listenersLoc.get(k));
            }
        } else {
            passFailure((L)listenerLoc);
        }
    }
    
    protected boolean setHasValue(Throwable newExc) {
        if (_hasValue) {
            if (exc == null) {
                throw new IllegalStateException("value set already: "+value);
            } else {
                throw new IllegalStateException("exception set already: "+exc);
            }
        }
        _hasValue = true;
        notifyAll();
        listener = null;
        return false;
    }
    

    // ================= Promise implementation

    public void _addListener(L sink) {
        boolean _hasValueLoc = addListenerGetHasValue(sink);
        if (_hasValueLoc) {
            if (exc != null) {
                passResult(sink);
            } else {
                passFailure(sink);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private synchronized boolean addListenerGetHasValue(L sink) {
        if (_hasValue) {
            return true;
        }
        if (listener == null) {
            listener = sink;
            return false;
        }
        if (listener instanceof ArrayList<?>) {
            ((ArrayList<L>) listener).add(sink);
        } else {
            ArrayList<L> listenersLoc = new ArrayList<L>();
            listenersLoc.add((L) listener);
            listenersLoc.add(sink);
            listener = listenersLoc;
        }
        return false;
    }

}
