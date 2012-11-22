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

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * General dataflow node with several inputs and outputs.
 * Firing occur when all inputs are filled.
 * Typical use case is:
 *  - create 1 or more pins for inputs and/or outputs
 *  - redefine abstract method act()
 */
public abstract class DataflowNode extends Link {
	static final int allOnes=0xFFFFFFFF;
	private Lock lock = new ReentrantLock();
	private Throwable exc=null;
    private Pin head; // the head of the list of Pins
    private int pinCount=0;
    private int pinMask=0; // mask with 1 for all existing pins
    private int readyPins=0;  // mask with 1 for all ready pins
    private final Task task; 
    private boolean fired=false; // true when this actor runs
    
    public DataflowNode(Executor executor) {
        task=new ActorTask(executor);
    }

    public DataflowNode() {
        task=new ActorTask();
    }

    public  void sendFailure(Throwable exc) {
        boolean doFire;       
        lock.lock();
        try {
            if (this.exc!=null) {
                return; // only first failure is processed 
            }
            this.exc=exc;
            if (fired) {
                doFire=false;
            } else {
                doFire=fired=true;
            }
        } finally {
          lock.unlock();
        }
        if (doFire) {
            fire();
        }
    }

    /**
     * @return true if the actor has all its pins on and so is ready for execution
     */
    private final boolean allReady() {
		return readyPins==pinMask;
	}
    
    private final void fire() {
        //System.out.println("fire()");
        task.fire();
    }
    
    //========= backend
    
    /**
     * reads extracted tokens from places and performs specific calculations 
     */
    protected abstract void act();

    protected void handleException(Throwable exc) {
        System.err.println("DataflowNode.handleException:"+exc);
        exc.printStackTrace();
    }

    /** 
     * Extracts tokens from pins.
     * Extracted tokens are expected to be used used in the act() method.
     */
    private final void consumeTokens() {
        for (Pin pin=head; pin!=null; pin=pin.next) {
            pin.consume();
        }
    }
    
    //====================== inner classes
    
    /** We could extend this class from Task, but define separate class to 
     * minimize class hierarchy
     */
    private class ActorTask extends Task {
        
        public ActorTask() {
        }

        public ActorTask(Executor executor) {
            super(executor);
        }

        /** loops while all pins are ready
         */
        @Override
        public void run() {
            //System.out.println("ActorTask run");
            execLoop:
            try {
                // the loop slightly unrolled to have only one
                // synchronized statement in the loop
                lock.lock();
                try {
                    if (exc!=null) {
                        break execLoop; // fired remains true, preventing subsequent execution
                    }
                } finally {
                  lock.unlock();
                }
                //System.out.println("act0 before");
                act();
                for (;;) {
                    lock.lock();
                    try {
                        consumeTokens();
                        if (!allReady()) {
                            fired = false; // allow firing
                            //System.out.println("act !allReady fired = false");
                            return;
                        }
                        //System.out.println("act allReady");
                        if (exc!=null) {
                            break execLoop; // fired remains true, preventing subsequent execution
                        }
                    }
                    finally {
                      lock.unlock();
                    }
                    //System.out.println("act before");
                    act();
                }
            } catch (Throwable e) {
                exc=e;
            }
            try {
                handleException(exc);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Basic place for input tokens.
     * Initial state should be empty, to prevent premature firing.
     */
    protected abstract class Pin {
        private Pin next; // link to list
    	private final int pinBit; // distinct for all other pins of the node 

    	protected Pin(){
            lock.lock();
            try {
                int count = pinCount;
                if (count==32) {
                  throw new IllegalStateException("only 32 pins could be created");
                }
                next=head; head=this; // register itself in the pin list
                pinBit = 1<<count;
                pinMask=pinMask|pinBit;
                pinCount++;
            } finally {
              lock.unlock();
            }
        }

    	/**
    	 * sets pin's bit on and fires task if all pins are on
    	 *  @return true if actor became ready and must be fired
    	 */
        protected boolean turnOn() {
            readyPins |= pinBit;
            //System.out.print("turnOn "+fired+" "+allReady());
            if (fired || !allReady()) {
                //System.out.println(" => false");
                return false;
            }
            fired = true; // to prevent multiple concurrent firings
            //System.out.println(" => true");
            return true;
        }

        /**
         * sets pin's bit off
         */
        protected void turnOff() {
            //System.out.println("turnOff");
            readyPins &= ~pinBit;
        }

        /** Executed after token processing (method act).
         * Cleans reference to value, if any.
         * Sets state to off if no more tokens are in the place.
         * Should return quickly, as is called from the actor's synchronized block.
         */
        protected abstract void consume();
        
    }

    /**
     * holds tokens without data 
     */
    protected class Sema extends Pin {
        private int count=0;
        
        public Sema() {
        }

        public void up() {
            boolean doFire;
            lock.lock();
            try {
                count++;
                if (count!=1) {
                    return;
                }
                doFire=turnOn();
            } finally {
              lock.unlock();
            }
            if (doFire) {
                task.fire();
            }
        }

        public void up(int delta) {
            lock.lock();
            try {
                boolean wasOff=(count==0);
                count+=delta;
                if (wasOff && count>0) {
                    turnOn();
                }
            } finally {
              lock.unlock();
            }
        }

        public void down() {
            lock.lock();
            try {
                consume();
            }
            finally {
              lock.unlock();
            }
        }

        @Override
        protected void consume() {
            if (count==0) {
                return;
            }
            count--;
            if (count==0) {
                turnOff();
            }
        }
    }

    /**
     * Token storage with standard Port<T> interface.
     * By default, it has place for only one token.
     * @param <T> type of accepted tokens.
     */
    public class Input<T> extends Pin implements StreamPort<T>{
        /** extracted token */
        private T value=null;
        private boolean closeRequested=false;

        @Override
        public void send(T token) {
            if (token==null) {
                throw new NullPointerException();
            }
            boolean doFire;
            lock.lock();
            try {
                if (closeRequested) {
                    throw new IllegalStateException("closed already");
                }
                if (value==null) {
                    value=token;
                    doFire=turnOn();
                } else {
                    add(token);
                    return; // is On already
                }
            } finally {
              lock.unlock();
            }
            if (doFire) {
                fire();
            }
        }

        /** Signals the end of the stream. 
         * Turns this pin on. Removed value is null 
         * (null cannot be send with StreamInput.add(message)).
         */
        @Override
        public void close() {
            boolean doFire;
            lock.lock();
            try {
                if (closeRequested) {
                    return;
                }
                closeRequested=true;
                //System.out.println("close()");
                doFire=turnOn();
            } finally {
              lock.unlock();
            }
            if (doFire) {
                fire();
            }
        }

        public boolean isClosed() {
            lock.lock();
            try {
                return closeRequested;
            } finally {
              lock.unlock();
            }
        }

        /**
         * saves passed token
         * @param newToken
         */
        protected void add(T newToken) {
            throw new IllegalStateException();
        }
                

        public T get() {
            return value;
        }

        //===================== backend
        
        /**
         * removes token from the storage
         * @return removed token
         */
        protected T poll() {
            return null;
        }

        @Override
        protected void consume() {
            boolean wasNull=(value==null);
            value = poll();
            if (value!=null) {
                return; // continue processing
            }
            // no more tokens; check closing
            if (wasNull) {
                turnOff(); // closing processed already
            }
            if (!closeRequested) {
                turnOff(); // closing not requested
            }
            // else make one more round with message==null
        }
    }

    /** A place for single unremovable token of type <T>
     * @param <T> 
     */
    public class ConstInput<T> extends Input<T> {

        /** restores value
         */
        @Override
        protected T poll() {
            return get();
        }
    }

    /** Scalar Input which also redirects failures 
     */
    public class CallbackInput<T> extends Input<T> implements Callback<T> {
        @Override
        public void sendFailure(Throwable exc) {
            DataflowNode.this.sendFailure(exc);
        }
    }
        
    /** A Queue of tokens of type <T>
     * @param <T> 
     */
    public class StreamInput<T> extends Input<T> {
        private Queue<T> queue;

        public StreamInput() {
            this.queue = new LinkedList<T>();
        }

        public StreamInput(Queue<T> queue) {
            this.queue = queue;
        }

        @Override
        protected void add(T token) {
            queue.add(token);
        }

        @Override
		public T poll() {
            return queue.poll();
        }
    }

    /**
     * 
     * This pin carries demand(s) of the result.
     * Demand is two-fold: it is a pin, so firing possible only if
     * someone demanded the execution, and it holds consumer's port where
     * the result should be sent. 
     * @param <R>  type of result
     */
    public class Demand<R> extends Pin implements EventSource<R, Callback<R>>, Callback<R> {
        private Promise<R> listeners=new Promise<R>();

        /** indicates a demand
         * @param sink Port to send the result
         * @return 
         */
        @Override
        public EventSource<R, Callback<R>> addListener(Callback<R> sink) {
        	boolean doFire;
            lock.lock();
            try {
                listeners.addListener(sink);
                doFire=turnOn();
            } finally {
              lock.unlock();
            }
            if (doFire) {
                task.fire();
            }
            return this;
    	}

    	/** satisfy demand(s)
    	 */
    	@Override
		public void send(R m) {
			listeners.send(m);
		}

        @Override
        public void sendFailure(Throwable exc) {
            listeners.sendFailure(exc);
        }

        /**
         * demands are not arguments, need not to be extracted
         */
        @Override
        protected void consume() {}
    }
}