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

/**
 * An actor is like a Petri Net trasnsition with own places for tokens.
 * Shared places cannot be represented directly. To some extent, the role
 * of shared places is played by {@link org.df4j.core.ext.Dispatcher}
 *
 * Own places can be of 2 sorts: carrying colorless tokens (without information,
 * like Starter and Semafor, and and carrying colored tokens, which are references to arbitrary objects.
 *
 * Actor is started when all its places are not empty (contain tokens). Excecution means execution
 * its (@link {@link BaseActor#act()} method on the executor set by {@link #setExecutor} method.
 */
public abstract class BaseActor {

    protected final Transition transition = new Transition();

    protected Pin controlPin = new Pin();

    {
        controlPin.turnOn();
    }

    /**
     * invoked when all transition transition are ready,
     * and method run() is to be invoked.
     * Safe way is to submit this instance as a Runnable to an Executor.
     * Fast way is to invoke it directly, but make sure the chain of
     * direct invocations is short to avoid stack overflow.
     */
    protected abstract void fire();

    // ========= backend
    // ====================== inner classes

    public class Pin extends Transition.Pin {
        protected Pin() {
            transition.super();
        }

        public void turnOnAndFire() {
            boolean on = super.turnOn();
            if (on) {
                controlPin.turnOff();
                fire();
            }
        }

        @Override
        protected void purge() {
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
                turnOnAndFire();
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
            turnOnAndFire();
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

    //=============================== streams

    /*******************************************************
     * A Queue of tokens of type <T>
     *
     * @param <T>
     */
    public class StreamInput<T> extends Input<T> implements StreamPort<T> {
        protected Deque<T> queue;
        private boolean closeRequested = false;

        public StreamInput () {
            this.queue = new ArrayDeque<T>();
        }

        public StreamInput (int capacity) {
            this.queue = new ArrayDeque<T>(capacity);
        }

        public StreamInput(Deque<T> queue) {
            this.queue = queue;
        }

        protected int size() {
            return queue.size();
        }

        @Override
        public synchronized void post(T token) {
            if (token == null) {
                throw new NullPointerException();
            }
            if (closeRequested) {
                throw new IllegalStateException("closed already");
            }
            if (value == null) {
                value = token;
                turnOnAndFire();
            } else {
                queue.add(token);
            }
        }

        /**
         * Signals the end of the stream. Turns this pin on. Removed value is
         * null (null cannot be send with StreamInput.add(message)).
         */
        @Override
        public synchronized void close() {
            if (closeRequested) {
                return;
            }
            closeRequested = true;
            if (value == null) {
                turnOnAndFire();
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
        protected synchronized void purge() {
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