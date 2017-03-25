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

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * General dataflow node with several inputs and outputs.
 * Firing occur when all inputs are filled.
 * Typical use case is:
 *  - create 1 or more pins for inputs and/or outputs
 *  - redefine abstract method act()
 */
public abstract class Actor implements Runnable {
    protected ForkJoinPool commonPool=ForkJoinPool.commonPool();

    private Pin head; // the head of the list of Pins
    private int pinCount = 1; // fire bit allocated
    private AtomicInteger blockedPins = new AtomicInteger(0); // mask with 0 for ready pins, 1 for blocked
    
    /** invoked when all transition pins are ready,
     *  and method run() is to be invoked.
     *  Safe way is to submit this instance as a Runnable to an Executor.
     *  Fast way is to invoke it directly, but make sure the chain of
     *  direct invocations is short to avoid stack overflow.
     */
    protected void fire() {
        commonPool.execute(this);
    }    

    protected synchronized void consumeTokens() {
        for (Pin pin = head; pin != null; pin = pin.next) {
            pin._purge();
        }
    }

    /**
     * loops while all pins are ready
     */
    @Override
    public void run() {
        try {
        	for (;;) {
                act();
                consumeTokens();
                if (blockedPins.updateAndGet(pins -> pins==1? 1: pins & ~1) != 1) {
            		return;
            	}
        	}
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
     * Basic place for input tokens. Initial state should be empty, to prevent
     * premature firing.
     */
    protected  abstract class Pin {
        Pin next = null; // link to pin list
        final int pinBit; // distinct for all other pins of the node

        public Pin() {
            synchronized(Actor.this) {
                if (pinCount == 32) {
                    throw new IllegalStateException(
                            "only 32 pins could be created");
                }
                pinBit = 1 << pinCount; // assign next pin number
                _turnOff(); // mark this pin as blocked
                pinCount++;
                // register itself in the pin list
                if (head == null) {
                    head = this;
                    return;
                }
                Pin prev = head;
                while (prev.next != null) {
                    prev = prev.next;
                }
                prev.next = this;
            }
        }

        /** unlock pin by setting it to 0
         * @return true if transition fired emitting control token
         */
		protected final boolean _turnOn() {
			return blockedPins.updateAndGet(pins -> {
				int newPins = pins & ~pinBit;
				if (newPins == 0) {
					newPins = 1;
				}
				return newPins;
			}) == 1;
		}

        /**
         * lock pin by setting it to 1
         * called when a token is consumed and the pin become empty
         */
        protected final void _turnOff() {
        	blockedPins.updateAndGet(pins -> pins | pinBit);
        }

        /**
         * Executed after token processing (method act). Cleans reference to
         * value, if any. Signals to set state to off if no more tokens are in
         * the place. Should return quickly, as is called from the actor's
         * synchronized block. 
         */
        protected abstract void _purge();
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
        public void post(T token) {
            if (token == null) {
                throw new NullPointerException();
            }
            boolean doFire;
            synchronized(this) {
                if (value != null) {
                    throw new IllegalStateException("token set already");
                }
                value = token;
                doFire = _turnOn();
            }
            if (doFire) {
                fire();
            }
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

        // TODO why return boolean result
        @Override
        protected synchronized void _purge() {
            if (pushback) {
                pushback = false;
                // value remains the same, the pin remains turned on
            } else {
                value = null;
                _turnOff();
            }
        }
    }    
    
    /*******************************************************
     * Counting semaphore
     * holds token counter without data.
     * counter can be negative.
     */
    public class Semafor extends Pin {
        private int count;

        public Semafor() {
            this.count = 0;
        }

        public Semafor(int count) {
            if (count > 0) {
                throw new IllegalArgumentException("initial counter cannot be positive");
            }
            this.count = count;
        }

        /** increments resource counter by 1 */
        public void up() {
            boolean doFire;
            synchronized(this) {
                count++;
                if (count != 1) {
                    return;
                }
                doFire = _turnOn();
            }
            if (doFire) {
                fire();
            }
        }

        /** increments resource counter by delta */
        public void up(int delta) {
            boolean doFire;
            synchronized(this) {
                boolean wasOff = (count <= 0);
                count += delta;
                boolean isOff = (count <= 0);
                if (wasOff == isOff) {
                    return;
                }
                if (isOff) {
                    _turnOff();
                    return;
                }
                doFire = _turnOn();
            }
            if (doFire) {
                fire();
            }
        }

        @Override
        protected void _purge() {
            if (--count == 0) {
                _turnOff();
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
            this.queue = new LinkedList<T>();
        }

        public StreamInput(Deque<T> queue) {
            this.queue = queue;
        }

        @Override
        public void post(T token) {
            if (token == null) {
                throw new NullPointerException();
            }
            boolean doFire;
            synchronized(this) {
                if (closeRequested) {
                    throw new IllegalStateException("closed already");
                }
                if (value == null) {
                    value = token;
                    doFire = _turnOn();
                } else {
                    queue.add(token);
                    return; // is On already
                }
            }
            if (doFire) {
                fire();
            }
        }

        /**
         * Signals the end of the stream. Turns this pin on. Removed value is
         * null (null cannot be send with StreamInput.add(message)).
         */
        @Override
        public void close() {
            boolean doFire;
            synchronized(this) {
                if (closeRequested) {
                    return;
                }
                closeRequested = true;
                if (value == null) {
                    doFire = _turnOn();
                } else {
                    return; // is On already
                }
            }
            if (doFire) {
                fire();
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
                _turnOff();  
            }
            // else process closing: value is null, the pin remains turned on
        }

        public synchronized boolean  isClosed() {
            return closeRequested && (value == null);
        }
    }
}