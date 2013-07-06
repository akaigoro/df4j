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
    public void post(R m) {
        Object listenerLoc = setValueGetListener(m);
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

    @SuppressWarnings("unchecked")
    @Override
    public void postFailure(Throwable exc) {
        Object listenerLoc = setFailureGetListener(exc);
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

    private synchronized Object setValueGetListener(R m) {
        if (_hasValue) {
            Object v = this.exc != null ? this.exc : value;
            throw new IllegalStateException("value set already: " + v);
        }
        _hasValue = true;
        value = m;
        notifyAll();
        Object listenerLoc = listener;
        listener = null;
        return listenerLoc;
    }

    private synchronized Object setFailureGetListener(Throwable exc) {
        if (_hasValue) {
            Object v = this.exc != null ? this.exc : value;
            throw new IllegalStateException("value set already: " + v);
        }
        _hasValue = true;
        this.exc = exc;
        notifyAll();
        Object listenerLoc = listener;
        listener = null;
        return listenerLoc;
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
