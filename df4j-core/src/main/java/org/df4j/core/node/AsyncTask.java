/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * one-shot asynchronous call
 */
public abstract class AsyncTask implements Runnable {

     /**
     * total number of created pins
     */
    protected AtomicInteger pinCount = new AtomicInteger();

    /**
     * total number of created pins
     */
    protected AtomicInteger blockedPinCount = new AtomicInteger();

    /** the list of all Pins */
    protected final Collection<Connector> connectors;

    protected AsyncTask(Collection<Connector> connectors) {
        this.connectors = connectors;
    }

    protected AsyncTask() {
        this(new ArrayList<>(6));
    }

    protected void unRegister(Connector connector) {
        connectors.remove(connector);
    }

    protected Executor executor = ForkJoinPool.commonPool();

    /**
     * assigns Executor
     */
    public void setExecutor(Executor exec) {
        this.executor = exec;
    }

    public Executor getExecutor() {
        return executor;
    }

    public void runOn(Executor executor) {
        executor.execute(this);
    }

    /**
     * invoked when all asyncTask asyncTask are ready,
     * and method run() is to be invoked.
     * Safe way is to submit this instance as a Runnable to an Executor.
     * Fast way is to invoke it directly, but make sure the chain of
     * direct invocations is short to avoid stack overflow.
     */
    protected void fire() {
        getExecutor().execute(this);
    }

    @Override
    public void run() {
        try {
            act();
        } catch (Throwable e) {
            // TODO move to failed state
            System.err.println("Error in actor " + getClass().getName());
            e.printStackTrace();
        }
    }

    /**
     * reads extracted tokens from places and performs specific calculations
     *
     * @throws Exception
     */
    protected abstract void act() throws Exception;

    /**
     * Basic class for all connectors (places for tokens).
     * Asynchronous version of binary semaphore.
     *
     * initially in non-blocked state
     */
    public class Connector {
        int pinNumber; // distinct for all other connectors of this node
        boolean blocked;

        public Connector(boolean blocked) {
            this.pinNumber = pinCount.getAndIncrement();
            this.blocked = blocked;
            if (blocked) {
                blockedPinCount.incrementAndGet();
            }
            connectors.add(this);
        }

        /**
         * by default, initially in blocked state
         */
        public Connector() {
            this(true);
        }

        public boolean isBlocked() {
            return blocked;
        }

        public int getPinNumber() {
            return pinNumber;
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

        public void turnOn() {
            if (!blocked) {
                return;
            }
            blocked = false;
            long res = blockedPinCount.decrementAndGet();
            if (res == 0) {
                fire();
            }
        }

        /**
         * Executed after token processing (method act). Cleans reference to
         * value, if any. Signals to set state to off if no more tokens are in
         * the place. Should return quickly, as is called from the actor's
         * synchronized block.
         */
        public void purge() {
        }

        protected void unRegister() {
            if (blocked) {
                turnOn();
            }
            AsyncTask.this.unRegister(this);
        }
    }

}