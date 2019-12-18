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

import org.df4j.core.protocol.SignalStream;
import org.df4j.core.util.Utils;

import java.util.concurrent.Executor;

/**
 * {@link BasicBlock} is a component of {@link Dataflow} graph.
 *  It plays the same role as basic blocks in a flow chart
 *  (see <a href="https://en.wikipedia.org/wiki/Flowchart">https://en.wikipedia.org/wiki/Flowchart</a>).
 * Control flow is provided with the method {@link BasicBlock#awake()}.
 * Unlike basic blocks in traditional flow charts, {@link BasicBlock}s in the same {@link Dataflow} graph can run in parallel.
 */
public abstract class BasicBlock implements SignalStream.Subscriber {
    protected final Dataflow dataflow;
    /**
     * blocked initially, until {@link #awake} called.
     */
    private Port ports = null;
    private int blockingPortCount = 0;
    private Executor executor;
    private Port controlport = new ControlPort();

    protected BasicBlock(Dataflow dataflow) {
        this.dataflow = dataflow;
        dataflow.enter();
    }

    /**
     * passes a control token to this {@link BasicBlock}.
     * This token is consumed when this block is submitted to an executor.
     */
    public void awake() {
        synchronized(controlport) {
            if (dataflow.isCompleted()) {
                return;
            }
            if (controlport.unblock()) return;
        }
        controlport.decBlocking();
    }

    /**
     * finishes parent activity normally.
     */
    public synchronized void stop() {
        dataflow.leave();
    }

    /**
     * finishes parent activity exceptionally.
     * @param ex the exception
     */
    protected synchronized void stop(Throwable ex) {
        dataflow.onError(ex);
    }

    public void setExecutor(Executor exec) {
        this.executor = exec;
    }

    private void incBlocking() {
        blockingPortCount++;
    }

    protected synchronized Executor getExecutor() {
        if (executor == null) {
            executor = Utils.getThreadLocalExecutor();
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
        executor.execute(this::run);
    }

    /**
     * the main entry point.
     * Overwrite only to declare different kind of node.
     */
    protected void run() {
        try {
            runAction();
        } catch (Throwable e) {
            stop(e);
        }
    }

    /**
     * User's action.
     * @throws Throwable when thrown, this node is considered failed.
     */
    protected abstract void runAction() throws Throwable;

    /**
     * Basic class for all ports (places for tokens).
     * Has 2 states: ready or blocked.
     * When all ports become unblocked, method {@link BasicBlock#fire()} is called.
     * This resembles firing of a Petri Net transition.
     */
    public abstract class Port {
        private boolean ready;
        private Port next;

        public Port(boolean ready) {
            this.ready = ready;
            if (!ready) {
                incBlocking();
            }
            next = ports;
            ports = this;
        }

        public synchronized boolean isReady() {
            return ready;
        }

        /**
         * sets this port to a blocked state.
         */
        public synchronized void block() {
            if (!ready) {
                return;
            }
            ready = false;
            incBlocking();
        }

        protected void decBlocking() {
            synchronized(BasicBlock.this) {
                if (blockingPortCount == 0) {
                    throw new IllegalStateException();
                }
                blockingPortCount--;
                if (blockingPortCount > 0) {
                    return;
                }
                if (executor == null) {
                    executor = Utils.getThreadLocalExecutor();
                }
                controlport.block();
            }
            fire();
        }

        /**
         * sets this port to unblocked state.
         * If all ports become unblocked,
         * this block is submitted to the executor.
         */
        protected boolean unblock() {
            if (ready) {
                return true;
            }
            if (blockingPortCount == 0) {
                throw new InternalError(getClass().getName()+": blockedPortCount==0 but blocked port exists ");
            }
            ready = true;
            return false;
        }
    }

    private class ControlPort extends Port {
        public ControlPort() {
            super(false);
        }
    }
}