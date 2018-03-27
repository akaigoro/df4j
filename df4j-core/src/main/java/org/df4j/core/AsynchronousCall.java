/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core;

import java.io.Closeable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AsynchronousCall is like a Petri Net trasnsition with own places for tokens,
 * where places can keep at most one token, and which is not reused: after firing,
 * another arguments cannot be supplied and another firing cannot occur.
 *
 * Actor is started when all its places are not empty (contain tokens). Excecution means execution
 * its (@link {@link AsynchronousCall#act()} method on the executor set by {@link #setExecutor} method.
 */
public abstract class AsynchronousCall implements Runnable {
    public static final Executor directExecutor = task->task.run();

    protected final Transition transition = new Transition();
    protected final AtomicReference<Executor> executor = new AtomicReference<>();

    /**
     * assigns Executor
     * returns previous executor
     */
    public Executor setExecutor(Executor exec) {
        Executor res = this.executor.getAndUpdate((prev)->exec);
        return res;
    }

    protected Executor getExecutor() {
        Executor exec = executor.get();
        return exec;
    }

    protected Executor getExecutorNotNull() {
        Executor exec = executor.get();
        if (exec == null) {
            exec = executor.updateAndGet((prev)->prev==null? ForkJoinPool.commonPool():prev);
        }
        return exec;
    }

    public void useDirectExecutor() {
        setExecutor(directExecutor);
    }

    /**
     * invoked when all transition transition are ready,
     * and method run() is to be invoked.
     * Safe way is to submit this instance as a Runnable to an Executor.
     * Fast way is to invoke it directly, but make sure the chain of
     * direct invocations is short to avoid stack overflow.
     */
    protected void fire() {
        Executor executor = getExecutorNotNull();
        executor.execute(this);
    }

    /**
     * initially in non-blocing state
     */
    public class OpenPin extends Transition.Pin {
        protected OpenPin() {
            transition.super();
        }

        public void turnOn() {
            boolean on = super._turnOn();
            if (on) {
                fire();
            }
        }

        @Override
        protected void purge() {
        }
    }

    /**
     * initially in blocing state
     */
    public class Pin extends OpenPin {
        public Pin() {
            turnOff();
        }
    }
        /**
     * Counting semaphore
     * holds token counter without data.
     * counter can be negative.
     */
    public class Semafor extends Pin implements Closeable {
        private volatile boolean closed = false;
        private long count = 0;

        public Semafor() {
        }

        public boolean isClosed() {
            return closed;
        }

        public long getCount() {
            return count;
        }

        @Override
        public synchronized void close() {
            closed = true;
            count = 0;
            turnOff(); // and cannot be turned on
        }

        /** increments resource counter by delta */
        public synchronized void release(long delta) {
            if (closed) {
                throw new IllegalStateException("closed already");
            }
            if (delta < 0) {
                throw new IllegalArgumentException("resource counter delta must be >= 0");
            }
            long prev = count;
            count+= delta;
            if (prev <= 0 && count > 0 ) {
                turnOn();
            }
        }

        /** increments resource counter by delta */
        protected synchronized void aquire(long delta) {
            if (delta < 0) {
                throw  new IllegalArgumentException("resource counter delta must be >= 0");
            }
            long prev = count;
            count -= delta;
            if (prev > 0 && count <= 0 ) {
                turnOff();
            }
        }

        @Override
        protected synchronized void purge() {
            aquire(1);
        }
    }

    //=============================== scalars

    /*******************************************************
     * Token storage with standard Port<T> interface. It has place for only one
     * token, which is never consumed.
     *
     * @param <T>
     *     type of accepted tokens.
     */
    public class ConstInput<T> extends Pin implements Port<T> {

        /** extracted token */
        public T value = null;

        public T get() {
            return value;
        }

        /**
         *  @throws NullPointerException
         *  @throws IllegalStateException
         */
        @Override
        public synchronized void post(T token) {
            if (token == null) {
                throw new IllegalArgumentException();
            }
            if (value != null) {
                throw new IllegalStateException("token set already");
            }
            value = token;
            turnOn();
        }

        /**
         * pin bit remains ready
         */
        @Override
        protected void purge() {
        }
    }

    /**
     * Token storage with standard Port<T> interface.
     * It has place for only one token.
     *
     * @param <T>
     *            type of accepted tokens.
     */
    public class Input<T> extends ConstInput<T> implements Port<T> {
        protected boolean pushback = false; // if true, do not consume

        // ===================== backend

        protected void pushback() {
            if (pushback) {
                throw new IllegalStateException();
            }
            pushback = true;
        }

        protected synchronized void pushback(T value) {
            if (pushback) {
                throw new IllegalStateException();
            }
            pushback = true;
            this.value = value;
        }

        @Override
        protected synchronized void purge() {
            if (pushback) {
                pushback = false;
                // value remains the same, the pin remains turned on
            } else {
                value = null;
                turnOff();
            }
        }
    }
}