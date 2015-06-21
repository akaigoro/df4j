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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ForkJoinPool;

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
    private int blockedPins = 0; // mask with 0 for ready pins, 1 for blocked
    private RequestingInput<?> reqHead;
    
    /** lock pin */
    private final void _pinOff(int pinBit) {
        blockedPins |= pinBit;
    }

    /** unlock pin */
    private final void _pinOn(int pinBit) {
        blockedPins &= ~pinBit;
    }

    private final void _lockFire() {
        _pinOff(1);
    }

    private final void _unlockFire() {
        _pinOn(1);
    }

    /**
     * @return true if the actor has all its input pins on
     * and so is ready for execution
     */
    private final boolean _allInputsReady() {
        for (RequestingInput<?> pin = reqHead; pin != null; pin = pin.next) {
            if (pin.value==null) {
                return false;
            }
        }
        return (blockedPins | 1) == 1;
    }

    /** invoked when all transition direct pins ready.
     *  Starts requesting pins.
     */
    private void fire1() {
        for (RequestingInput<?> pin = reqHead; pin != null; pin = pin.next) {
            if (pin.value==null) {
                pin.makeRequest();
                return;
            }
        }
        fire();
    }

    /** invoked when all transition pins are ready,
     *  and method run() is to be invoked.
     *  Safe way is to submit this instance as a Runnable to an Executor.
     *  Fast way is to invoke it directly, but make sure the chain of
     *  direct invocations is short to avoid stack overflow.
     */
    protected void fire() {
        commonPool.execute(this);
    }    

    private synchronized boolean consumeTokens() {
        for (RequestingInput<?> pin = reqHead; pin != null; pin = pin.next) {
            pin.purge();
        }
        for (Pin pin = head; pin != null; pin = pin.next) {
            pin._purge();
        }
        boolean doFire = _allInputsReady();
        if (!doFire) {
            _unlockFire(); // allow firing
        } 
        return doFire;
    }

    public String getStatus() {
        StringBuilder sb = new StringBuilder();
        synchronized(Actor.this) {
            sb.append("running:");
            sb.append(blockedPins & 1);
            for (Pin pin = head; pin != null; pin = pin.next) {
                sb.append(", ");
                sb.append(pin.getClass().getSimpleName());
                sb.append("(bit:");
                sb.append(pin.pinBit);
                sb.append(", blocked:");
                sb.append((blockedPins & pin.pinBit) == 0 ? "0)" : "1)");
            }
        }
        return sb.toString();
    }

    /**
     * loops while all pins are ready
     */
    @Override
    public void run() {
        try {
            act();
            boolean  doFire = consumeTokens();
            if (doFire) {
                fire1();
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
    protected abstract class Pin {
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

        /**
         * sets pin's bit on and fires task if all pins are on
         * 
         * @return true if actor became ready and must be fired
         */
        protected final boolean _turnOn() {
            // System.out.print("turnOn "+fired+" "+allReady());
            _pinOn(pinBit);
            if (blockedPins == 0) {
                _lockFire(); // to prevent multiple concurrent firings
                // System.out.println(" => true");
                return true;
            } else {
                // System.out.println(" => false");
                return false;
            }
        }

        /**
         * sets pin's bit off
         */
        protected final void _turnOff() {
            // System.out.println("turnOff");
            _pinOff(pinBit);
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
        protected T value = null;
        
        protected T get() {
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
            synchronized(Actor.this) {
                if (value != null) {
                    throw new IllegalStateException("token set already");
                }
                value = token;
                doFire = _turnOn();
            }
            if (doFire) {
                fire1();
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

        protected void pushback(T value) {
            if (pushback) {
                throw new IllegalStateException();
            }
            pushback = true;
            this.value = value;
        }

        // TODO why return boolean result
        @Override
        protected void _purge() {
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

        protected Semafor() {
            this.count = 0;
        }

        protected Semafor(int count) {
            if (count > 0) {
                throw new IllegalArgumentException("initial counter cannot be positive");
            }
            this.count = count;
        }

        /** increments resource counter by 1 */
        public void up() {
            boolean doFire;
            synchronized(Actor.this) {
                count++;
                if (count != 1) {
                    return;
                }
                doFire = _turnOn();
            }
            if (doFire) {
                fire1();
            }
        }

        /** increments resource counter by delta */
        public void up(int delta) {
            boolean doFire;
            synchronized(Actor.this) {
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
                fire1();
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
    public class StreamInput<T> extends Input<T> implements StreamPort<T>, Iterable<T> {
        private Deque<T> queue;
        private boolean closeRequested = false;

        public StreamInput () {
            this.queue = new LinkedList<T>();
        }

        public StreamInput(Deque<T> queue) {
            this.queue = queue;
        }

        public T get() {
            return value;
        }

        @Override
        public void post(T token) {
            if (token == null) {
                throw new NullPointerException();
            }
            boolean doFire;
            synchronized(Actor.this) {
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
                fire1();
            }
        }

        /**
         * Signals the end of the stream. Turns this pin on. Removed value is
         * null (null cannot be send with StreamInput.add(message)).
         */
        @Override
        public void close() {
            boolean doFire;
            synchronized(Actor.this) {
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
                fire1();
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
        protected void pushback(T value) {
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
            synchronized(Actor.this) {
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
        protected void _purge() {
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

        public boolean isClosed() {
            synchronized(Actor.this) {
                return closeRequested && (value == null);
            }
        }

        @Override
        public Iterator<T> iterator() {
            // TODO Auto-generated method stub
            return queue.iterator();
        }
    }

    //=============================== Requesting Pins
    
    public class RequestingInput<T> implements Port<T> {
        protected RequestingInput<?> next = null; // link to pin list
        protected Port<Port<T>> sharedPlace;
        protected boolean pushback = false; // if true, do not consume
        protected T value;

        public RequestingInput(Port<Port<T>> sharedPlace) {
            this.sharedPlace=sharedPlace;
            synchronized(Actor.this) {
                // register itself in the pin list
                if (reqHead == null) {
                    reqHead = this;
                    return;
                }
                RequestingInput<?> prev = reqHead;
                while (prev.next != null) {
                    prev = prev.next;
                }
                prev.next = this;
            }
        }

        private void makeRequest() {
            sharedPlace.post(this);
        }

        public T get() {
            // TODO Auto-generated method stub
            return value;
        }

        // ===================== backend

        protected void pushback() {
            if (pushback) {
                throw new IllegalStateException();
            }
            pushback = true;
        }

        protected void pushback(T value) {
            if (pushback) {
                throw new IllegalStateException();
            }
            pushback = true;
            this.value = value;
        }

        protected void purge() {
            if (pushback) {
                pushback = false;
                // value remains the same, the pin remains turned on
            } else {
                value = null;
            }
        }

        @Override
        public void post(T message) {
            value=message;
            if (next==null) {
                fire1();
            } else {
                next.makeRequest();
            }
        }
    }
    
}