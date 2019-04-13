/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.asyncproc;

import java.util.ArrayList;

/**
 * This class is named following Petri Nets terminology.
 *
 * This class contains base class {@link Pin} for connectors (places),
 * and a mechanism to count the number of blocked pins.
 * As soon as all connectors are unblocked, the method {@link #fire()} is called.
 */
public abstract class Transition {
    private static final Object[] emptyArgs = new Object[0];

    private int paramCount = 0;

    /**
     * first paramCount items are parameters, and the rest - pure locks
     */
    private ArrayList<Pin> locks = new ArrayList<>();

    /**
     * total number of created pins
     */
    private int blockedPinCount = 0;
    private int activePinCount = 0;

    public synchronized int lockCount() {
        return activePinCount;
    }

    protected int getParamCount() {
        return paramCount;
    }

    protected synchronized Object[] collectTokens() {
        if (paramCount == 0) {
            return emptyArgs;
        } else {
            Object[] args = new Object[paramCount];
            for (int k = 0; k < paramCount; k++) {
                args[k] = locks.get(k).current();
            }
            return args;
        }
    }

    protected void purgeAll() {
        for (int k = 0; k < locks.size(); k++) {
            locks.get(k).purge();
        }
    }

    /**
     * usually submits this object as a task to a thread pool
     */
    protected abstract void fire();

    /**
     * Basic class for all locks and connectors (places for tokens).
     * Can be considered as an asynchronous binary semaphore.
     * Has 2 states: blocked or unblocked.
     * When all pins become unblocked, method {@link #fire()} is called.
     * This resembles firing of a Petri Net transition.
     */
    public class Pin {
        protected int pinNumber; // distinct for all other connectors of this node
        protected boolean blocked;
        protected boolean completed = false;

        public Pin(boolean blocked) {
            register(blocked);
        }

        /**
         * by default, initially in blocked state
         */
        public Pin() {
            register(true);
        }

        private synchronized void register(boolean blocked) {
            this.blocked = blocked;
            if (blocked) {
                blockedPinCount++;
            }
            if (isParameter()) {
                locks.add(paramCount, this);
                paramCount++;
            } else {
                locks.add(this);
            }
            this.pinNumber = locks.size();
            activePinCount++;
        }

        public synchronized boolean isCompleted() {
            return completed;
        }

        protected boolean isParameter() {
            return false;
        }

        /** must be overriden in parameters
         *
         * @return curren value of the parameter
         */
        public Object current() {
            throw new UnsupportedOperationException();
        }

        /**
         * locks the pin
         * called when a token is consumed and the pin become empty
         */
        public synchronized void block() {
            if (completed) {
                return;
            }
            if (blocked) {
                return;
            }
            blocked = true;
            blockedPinCount++;
        }

        public void unblock() {
            synchronized (this) {
                if (completed) {
                    return;
                }
                if (!blocked) {
                    return;
                }
                blocked = false;
                blockedPinCount--;
                if (blockedPinCount > 0) {
                    return;
                }
            }
            fire();
        }

        protected synchronized void complete() {
            synchronized (this) {
                if (completed) {
                    return;
                }
                completed = true;
                activePinCount--;
                if (!blocked) {
                    return;
                }
                blocked = false;
                blockedPinCount--;
                if (blockedPinCount > 0) {
                    return;
                }
            }
            fire();
        }

        /**
         * Must be executed  before restart of the parent async action.
         * Invalidates current token
         * Signals to set state to off if no more tokens are in the place.
         */
        public void purge() { }
    }
}