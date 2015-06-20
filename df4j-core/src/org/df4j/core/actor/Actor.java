/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.actor;

import java.util.Deque;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.df4j.core.DFContext;

/**
 * General dataflow node with several inputs and outputs.
 * Firing occur when all inputs are filled.
 * Typical use case is:
 *  - create 1 or more pins for inputs and/or outputs
 *  - redefine abstract method act()
 */
public abstract class Actor {
    final SyncTask task;

    public Actor(Executor executor) {
        if (executor==null) {
            task = new SyncTask();
        } else {
            this.task = new ActorTask(executor);
        }
    }

    public Actor() {
        this.task = new ActorTask(DFContext.getCurrentExecutor());
    }

    protected void lockFire() {
        task.lockFire();
    }

    protected void unlockFire() {
        task.unlockFire();
    }

    // ========= backend

    /**
     * reads extracted tokens from places and performs specific calculations
     * 
     * @throws Exception
     */
    protected abstract void act() throws Exception;

    // ====================== inner classes
    class SyncTask implements Runnable {
        final Lock lock = new ReentrantLock();
        Pin head; // the head of the list of Pins
        int pinCount = 1; // fire bit allocated
        int blockedPins = 0; // mask with 0 for ready pins, 1 for blocked

        public void fire() {
            run();
        }


        /** lock pin */
        final void pinOff(int pinBit) {
            blockedPins |= pinBit;
        }

        /** unlock pin */
        final void pinOn(int pinBit) {
            blockedPins &= ~pinBit;
        }

        final void _lockFire() {
            pinOff(1);
        }

        private final void _unlockFire() {
            pinOn(1);
        }

        /**
         * @return true if the actor has all its pins on and so is ready for
         *         execution
         */
        private final boolean allInputsReady() {
            return (blockedPins | 1) == 1;
        }

        final boolean allReady() {
            return blockedPins == 0;
        }

        protected final void lockFire() {
            lock.lock();
            try {
                _lockFire();
            } finally {
                lock.unlock();
            }
        }

        protected final void unlockFire() {
            boolean doFire;
            lock.lock();
            try {
                if (allInputsReady()) {
                    doFire=true;
                } else {
                    _unlockFire();
                    doFire=false;
                }
            } finally {
                lock.unlock();
            }
            if (doFire) {
                fire();
            }
        }

        public String getStatus() {
            StringBuilder sb = new StringBuilder();
            try {
                lock.lock();
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
            } finally {
                lock.unlock();
            }
            return sb.toString();
        }

        /**
         * loops while all pins are ready
         */
        @Override
        public void run() {
            try {
                for (;;) {
                    act();
                    lock.lock();
                    try {
                        // consume tokens
                        for (Pin pin = head; pin != null; pin = pin.next) {
                            if (!pin.consume()) {
                                pin.turnOff();
                            }
                        }
                        if (!allInputsReady()) {
                            _unlockFire(); // allow firing
                            return;
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (Throwable e) {
                System.err.println("Actor.act():" + e);
                e.printStackTrace();
            }
        }
    }

    class ActorTask extends SyncTask {
        protected final Executor executor;

        public ActorTask(Executor executor) {
            if (executor==null) {
                throw new NullPointerException();
            }
            this.executor = executor;
        }

        public ActorTask() {
            this.executor = DFContext.getCurrentExecutor();
        }

        /**
         * activates this task by sending it to the executor
         */
        public final void fire() {
            executor.execute(this);
        }
    }
    
    //=============================== inner classes
    
    public class ConstInput<T> extends org.df4j.core.actor.ConstInput<T> {
        public ConstInput() {
            super(Actor.this);
        }
    }
    
    public class Input<T> extends org.df4j.core.actor.Input<T> {
        public Input() {
            super(Actor.this);
        }
    }
    
    public class Lockup extends org.df4j.core.actor.Lockup {
        public Lockup() {
            super(Actor.this);
        }
    }
    
    public class Semafor extends org.df4j.core.actor.Semafor {
        public Semafor() {
            super(Actor.this);
        }
        public Semafor(int count) {
            super(Actor.this, count);
        }
   }
    
    public class StreamInput<T> extends org.df4j.core.actor.StreamInput<T> {
        public StreamInput() {
            super(Actor.this);
        }
        public StreamInput(Deque<T> queue) {
            super(Actor.this, queue);
        }
    }

}