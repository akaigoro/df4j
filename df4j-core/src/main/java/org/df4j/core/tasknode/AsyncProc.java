/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.tasknode;

import org.df4j.core.boundconnector.messagescalar.ScalarSubscriber;
import org.df4j.core.util.executor.CurrentThreadExecutor;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AsyncProc is an Asynchronous Procedure.
 *
 * It consists of asynchronous connectors, implemented as inner classes,
 * user-defined asynchronous procedure, and a mechanism to call that procedure
 * using supplied {@link java.util.concurrent.Executor} as soon as all connectors are unblocked.
 *
 * This class contains base classes for locks and connectors
 */
public abstract class AsyncProc implements Runnable {
    public static final Executor directExec = (Runnable r)->r.run();
    public static final CurrentThreadExecutor currentThreadExec = new CurrentThreadExecutor();
    public static final Executor asyncExec = ForkJoinPool.commonPool();
    public static final Executor newThreadExec = (Runnable r)->new Thread(r).start();
    private static InheritableThreadLocal<Executor> threadLocalExecutor = new InheritableThreadLocal<Executor>(){
        @Override
        protected Executor initialValue() {
            return asyncExec;
        }
    };
    private static final Object[] emptyArgs = new Object[0];

    /**
     * for debug purposes, call
     * <pre>
     *    setThreadLocalExecutor(AsyncProc.currentThreadExec);
     * </pre>
     * before creating {@link AsyncProc} instances.
     *
     * @param exec default executor
     */
    public static void setThreadLocalExecutor(Executor exec) {
        threadLocalExecutor.set(exec);
    }

    /**
     * the set of all b/w Pins
     */
    private HashSet<Lock> locks;
    /**
     * the set of all colored Pins, to form array of arguments
     */
    private ArrayList<ConstInput<?>> asyncParams;
    /**
     * total number of created pins
     */
    private AtomicInteger pinCount = new AtomicInteger();
    /**
     * total number of created pins
     */
    private AtomicInteger blockedPinCount = new AtomicInteger();

    private Executor executor;

    public void setExecutor(Executor exec) {
        this.executor = exec;
    }

    public synchronized Executor getExecutor() {
        if (executor == null) {
            Thread currentThread = Thread.currentThread();
            if (currentThread instanceof ForkJoinWorkerThread) {
                executor = ((ForkJoinWorkerThread)currentThread).getPool();
            } else {
                executor = threadLocalExecutor.get();
            }
        }
        return executor;
    }

    /**
     * invoked when all asyncTask asyncTask are ready,
     * and method run() is to be invoked.
     * Safe way is to submit this instance as a Runnable to an Executor.
     * Fast way is to invoke it directly, but make sure the chain of
     * direct invocations is short to avoid stack overflow.
     */
    protected void fire() {
        Executor executor = getExecutor();
        executor.execute(this);
    }

    protected abstract boolean isStarted();

    protected int getParamCount() {
        if (asyncParams == null) {
            return 0;
        }
        return asyncParams.size();
    }

    protected synchronized Object[] consumeTokens() {
        locks.forEach(Lock::purge);
        if (asyncParams == null) {
            return emptyArgs;
        }
        int paramCount = asyncParams.size();
        Object[] args = new Object[paramCount];
        for (int k = 0; k< paramCount; k++) {
            ConstInput<?> asyncParam = asyncParams.get(k);
            args[k] = asyncParam.next();
        }
        return args;
    }

    /**
     * Basic class for all locs and connectors (places for tokens).
     * Asynchronous version of binary semaphore.
     * <p>
     * initially in non-blocked state
     */
    private abstract class BaseLock {
        int pinNumber; // distinct for all other connectors of this node
        boolean blocked;

        public BaseLock(boolean blocked) {
            this.pinNumber = pinCount.getAndIncrement();
            this.blocked = blocked;
            if (blocked) {
                blockedPinCount.incrementAndGet();
            }
            register();
        }

        /**
         * by default, initially in blocked state
         */
        public BaseLock() {
            this(true);
        }

        public boolean isBlocked() {
            return blocked;
        }

        /**
         * locks the pin
         * called when a token is consumed and the pin become empty
         */
        public void turnOff() {
            if (blocked) {
                return;
            }
            blocked = true;
            blockedPinCount.incrementAndGet();
        }

        public boolean turnOn() {
            if (!blocked) {
                return false;
            }
            blocked = false;
            long res = blockedPinCount.decrementAndGet();
            if (res == 0) {
                fire();
            }
            return true;
        }

        abstract protected void register();

        abstract protected void unRegister();
    }


    /**
     * Basic class for all permission parameters (places for black/white tokens).
     * Asynchronous version of binary semaphore.
     * <p>
     * initially in non-blocked state
     */
    public class Lock extends BaseLock {

        public Lock(boolean blocked) {
            super(blocked);
        }

        public Lock() {
            super();
        }

        @Override
        protected void register() {
            if (locks == null) {
                locks = new HashSet<>();
            }
            locks.add(this);
        }

        protected void unRegister() {
            if (blocked) {
                turnOn();
            }
            locks.remove(this);
        }

        /**
         * Executed after token processing (method act). Cleans reference to
         * value, if any. Signals to set state to off if no more tokens are in
         * the place. Should return quickly, as is called from the actor's
         * synchronized block.
         */
        public void purge() {
        }
    }

    /**
     * Token storage with standard Subscriber&lt;T&gt; interface. It has place for only one
     * token, which is never consumed.
     *
     * @param <T>
     *     type of accepted tokens.
     */
    public class ConstInput<T> extends BaseLock
            implements ScalarSubscriber<T>  // to connect to a ScalarPublisher
    {
        protected Subscription subscription;
        protected boolean closeRequested = false;
        protected boolean cancelled = false;

        /** extracted token */
        protected boolean completed = false;
        protected T value = null;
        protected Throwable exception;

        public ConstInput() {
            super();
        }

        public synchronized T current() {
            if (exception != null) {
                throw new IllegalStateException(exception);
            }
            return value;
        }

        public T getValue() {
            return value;
        }

        public Throwable getException() {
            return exception;
        }

        public boolean isDone() {
            return completed || exception != null;
        }

        public T next() {
            return current();
        }

        @Override
        public void onNext(T message) {
            if (message == null) {
                throw new IllegalArgumentException();
            }
            if (isDone()) {
                throw new IllegalStateException("token set already");
            }
            value = message;
            turnOn();
        }

        @Override
        public void onError(Throwable throwable) {
            if (isDone()) {
                throw new IllegalStateException("token set already");
            }
            this.exception = throwable;
        }

        public synchronized void cancel() {
            if (subscription == null) {
                return;
            }
            Subscription subscription = this.subscription;
            this.subscription = null;
            cancelled = true;
            subscription.cancel();
        }

        @Override
        protected void register() {
            if (isStarted()) {
                throw new IllegalStateException("cannot register connector after start");
            }
            if (asyncParams==null) {
                asyncParams = new ArrayList<>();
            }
            asyncParams.add(this);
        }

        protected void unRegister() {
            if (isStarted()) {
                throw new IllegalStateException("cannot unregister connector after start");
            }
            if (blocked) {
                turnOn();
            }
            asyncParams.remove(this);
        }
    }
}