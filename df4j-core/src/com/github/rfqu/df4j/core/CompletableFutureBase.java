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
 * A kind of dataflow variable: single input, multiple asynchronous outputs.
 * Distributes received value among listeners.
 * Value (or failure) can only be assigned once. It is then saved, and 
 * listeners connected after the assignment still would receive it.
 * May connect actors.
 * <p>Promise plays the same role as {@link java.util.concurrent.Future},
 * but the result is sent to ports, registered as listeners using {@link #addListener}.
 * Registration can happen at any time, before or after the result is computed.
 * 
 * Also acts as a Furure and connects Actors and Threads.
 * Actors are allowed to send messages to it, but not to get from. 
 * Threads are allowed both to send and get.
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

    protected abstract void informResult(L listenerLoc);
    protected abstract void informFailure(L listenerLoc);

    /**
     * @return null if was interrupted
     */
    public synchronized R getResult() {
        return value;
    }

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
        if (value != null) {
            return value;
        } else if (exc != null) {
            throw new ExecutionException(exc);
        } else {
            throw new CancellationException();
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
     * Since this Future does not represent any task, this method is used to
     * interrupt waiting threads.
     */
    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        if (_hasValue) {
            throw new IllegalStateException("has value already");
        }
        _hasValue = true;
        notifyAll();
        return true;
    }

    @Override
    public synchronized boolean isCancelled() {
        return _hasValue && value == null && exc == null;
    }

    // ==================== Callback implementation

    /**
     * sends a message to this instance and then to its listeners.
     */
    @SuppressWarnings("unchecked")
    @Override
    public synchronized void post(R m) {
        Object listenerLoc = setValueGetListener(m);
        if (listenerLoc == null) {
            return;
        }
        if (listener instanceof ArrayList<?>) {
            ArrayList<L> listenersLoc=(ArrayList<L>) listener;
            for (int k=0; k<listenersLoc.size(); k++) {
                informResult(listenersLoc.get(k));
            }
        } else {
            informResult((L)listenerLoc);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized void postFailure(Throwable exc) {
        Object listenerLoc = setFailureGetListener(exc);
        if (listenerLoc == null) {
            return;
        }
        if (listener instanceof ArrayList<?>) {
            ArrayList<L> listenersLoc=(ArrayList<L>) listener;
            for (int k=0; k<listenersLoc.size(); k++) {
                informFailure(listenersLoc.get(k));
            }
        } else {
            informFailure((L)listenerLoc);
        }
    }

    private Object setValueGetListener(R m) {
        Object listenerLoc;
        synchronized (this) {
            if (_hasValue) {
                Object v = this.exc != null ? this.exc : value;
                throw new IllegalStateException("value set already: " + v);
            }
            _hasValue = true;
            value = m;
            notifyAll();
            /*
             * if (listener == null) { return; }
             */
            listenerLoc = listener;
            listener = null;
        }
        return listenerLoc;
    }

    private Object setFailureGetListener(Throwable exc) {
        Object listenerLoc;
        synchronized (this) {
            if (_hasValue) {
                Object v = this.exc != null ? this.exc : value;
                throw new IllegalStateException("value set already: " + v);
            }
            _hasValue = true;
            this.exc = exc;
            notifyAll();
            /*
             * if (listener == null) { return; }
             */
            listenerLoc = listener;
            listener = null;
        }
        return listenerLoc;
    }

    // ================= Promise implementation

    public void _addListener(L sink) {
        boolean _hasValueLoc = addListenerGetHasValue(sink);
        if (_hasValueLoc) {
            if (exc != null) {
                informResult(sink);
            } else {
                informFailure(sink);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private synchronized boolean addListenerGetHasValue(L sink) {
        if (!_hasValue) {
            return false;
        }
        if (listener == null) {
            listener = sink;
            return true;
        }
        if (listener instanceof ArrayList<?>) {
            ((ArrayList<L>) listener).add(sink);
        } else {
            ArrayList<L> listenersLoc = new ArrayList<L>();
            listenersLoc.add((L) listener);
            listenersLoc.add(sink);
            listener = listenersLoc;
        }
        return true;
    }

}
