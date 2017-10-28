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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import org.df4j.core.Transition.Pin;

/**
 * a Transition for heavy computations
 */
public abstract class Actor implements Runnable {

 //   public static final Executor directExecutor = (Runnable command)->command.run();
    public static final Executor directExecutor = (Runnable command)-> ForkJoinPool.commonPool().execute(command);

    /** mask with 0 for ready pin, 1 for blocked */
    protected final Transition transition = createTransition();

    /** can be overriden to use user defined Transition extention */
    protected Transition createTransition() {
        return new Transition();
    }

    /** assigns Executor
     * returns previous executor
     */
    public Executor setExecutor(Executor executor) {
        return transition.setExecutor(executor);
    }

    protected Executor getExecutor() {
        return transition.getExecutor();
    }

    /**
     * invoked when all transition transition are ready,
     * and method run() is to be invoked.
     * Safe way is to submit this instance as a Runnable to an Executor.
     * Fast way is to invoke it directly, but make sure the chain of
     * direct invocations is short to avoid stack overflow.
     */
    protected void fire() {
        Executor executor = transition.getExecutorNotNull();
        executor.execute(this);
    }

    protected void checkFire(boolean doFire) {
        if (doFire) {
            fire();
        }
    }

    /**
     * loops while all transition are ready
     */
    @Override
    public void run() {
        try {
            do {
                act();
            } while (transition.consumeTokens());
        } catch (Throwable e) {
            System.err.println("Actor.act():" + e);
            e.printStackTrace();
        }
    }

    // ========= backend

    /**
     * reads extracted tokens from places and performs specific calculations
     *
     * @throws Exception
     */
    protected abstract void act() throws Exception;

    // ====================== inner classes

    /**
     * Counting semaphore
     * holds token counter without data.
     * counter can be negative.
     */
    public class Semafor extends Pin {
        private long count;

        public Semafor(int count) {
            transition.super();
            if (count > 0) {
                throw new IllegalArgumentException("initial counter cannot be positive");
            }
            this.count = count;
        }

        public Semafor() {
            this(0);
        }

        /** increments resource counter by 1 */
        public void up() {
            checkFire(doUp());
        }

        protected synchronized boolean doUp() {
            count++;
            if (count != 1) {
                return false;
            }
            return turnOn();
        }

        /** increments resource counter by delta */
        public void up(long delta) {
            checkFire(doUp(delta));
        }

        protected synchronized boolean doUp(long delta) {
            boolean wasOff = (count <= 0);
            count += delta;
            boolean isOff = (count <= 0);
            if (wasOff == isOff) {
                return false;
            }
            if (isOff) {
                turnOff();
                return false;
            }
            return turnOn();
        }

        @Override
        protected synchronized void _purge() {
            if (--count == 0) {
                turnOff();
            }
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

        public ConstInput() {
            transition.super();
        }

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
        public void post(T token) {
            if (token == null) {
                throw new NullPointerException();
            }
            checkFire(doPost(token));
        }

        private synchronized boolean doPost(T token) {
            if (value != null) {
                throw new IllegalStateException("token set already");
            }
            value = token;
            return turnOn();
        }

        /**
         * pin bit remains ready
         */
        @Override
        protected void _purge() {
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
        protected synchronized void _purge() {
            if (pushback) {
                pushback = false;
                // value remains the same, the pin remains turned on
            } else {
                value = null;
                turnOff();
            }
        }
    }

    //=============================== streams

    /*******************************************************
     * A Queue of tokens of type <T>
     *
     * @param <T>
     */
    public class StreamInput<T> extends Input<T> implements StreamPort<T> {
        private Deque<T> queue;
        private boolean closeRequested = false;

        public StreamInput () {
            this.queue = new ArrayDeque<T>();
        }

        public StreamInput(Deque<T> queue) {
            this.queue = queue;
        }

        @Override
        public void post(T token) {
            if (token == null) {
                throw new NullPointerException();
            }
            checkFire(doPost(token));
        }

        protected synchronized boolean doPost(T token) {
            if (closeRequested) {
                throw new IllegalStateException("closed already");
            }
            if (value == null) {
                value = token;
                return turnOn();
            } else {
                queue.add(token);
                return false; // is On already
            }
        }

        /**
         * Signals the end of the stream. Turns this pin on. Removed value is
         * null (null cannot be send with StreamInput.add(message)).
         */
        @Override
        public void close() {
            checkFire(doClose());
        }

        protected synchronized boolean doClose() {
            if (closeRequested) {
                return false;
            }
            closeRequested = true;
            if (value == null) {
                return turnOn();
            } else {
                return false; // is On already
            }
        }

        @Override
        protected void pushback() {
            if (pushback) {
                throw new IllegalStateException();
            }
            pushback = true;
        }

        @Override
        protected synchronized void pushback(T value) {
            if (value == null) {
                throw new IllegalArgumentException();
            }
            if (!pushback) {
                pushback = true;
            } else {
                if (this.value == null) {
                    throw new IllegalStateException();
                }
                queue.addFirst(this.value);
                this.value = value;
            }
        }

        /**
         * attempt to take next token from the input queue
         *
         * @return true if next token is available, or if stream is closed false
         *         if input queue is empty
         */
        public boolean moveNext() {
            synchronized(this) {
                if (pushback) {
                    pushback = false;
                    return true;
                }
                boolean wasNotNull = (value != null);
                T newValue = queue.poll();
                if (newValue != null) {
                    value = newValue;
                    return true;
                } else if (closeRequested) {
                    value = null;
                    return wasNotNull;// after close, return true once, then
                    // false
                } else {
                    return false;
                }
            }
        }

        @Override
        protected synchronized void _purge() {
            if (pushback) {
                pushback = false;
                return; // value remains the same, the pin remains turned on
            }
            boolean wasNull = (value == null);
            value = queue.poll();
            if (value != null) {
                return; // the pin remains turned on
            }
            // no more tokens; check closing
            if (wasNull || !closeRequested) {
                turnOff();
            }
            // else process closing: value is null, the pin remains turned on
        }

        public synchronized boolean  isClosed() {
            return closeRequested && (value == null);
        }
    }
}