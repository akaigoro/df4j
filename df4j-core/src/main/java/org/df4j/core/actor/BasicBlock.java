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

import org.df4j.protocol.SignalStream;
import org.df4j.core.util.Utils;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link BasicBlock} is a component of {@link Dataflow} graph.
 *  It plays the same role as basic blocks in a flow chart
 *  (see <a href="https://en.wikipedia.org/wiki/Flowchart">https://en.wikipedia.org/wiki/Flowchart</a>).
 * Control flow is provided with the method {@link BasicBlock#awake()}.
 * Unlike basic blocks in traditional flow charts, {@link BasicBlock}s in the same {@link Dataflow} graph can run in parallel.
 */
public abstract class BasicBlock implements SignalStream.Subscriber {
    private final Lock bblock = new ReentrantLock();
    protected final Dataflow dataflow;
    /**
     * blocked initially, until {@link #awake} called.
     */
    private Port ports = null;
    private int blockingPortCount = 0;
    private Executor executor;
    private Port controlport = new ControlPort();

    protected BasicBlock(Dataflow dataflow) {
        if (dataflow == null) {
            throw new IllegalArgumentException();
        }
        this.dataflow = dataflow;
        dataflow.enter();
    }

    /**
     * passes a control token to this {@link BasicBlock}.
     * This token is consumed when this block is submitted to an executor.
     */
    public void awake() {
        bblock.lock();
        try {
            if (dataflow.isCompleted()) {
                return;
            }
        } finally {
            bblock.unlock();
        }
        controlport.unblock();
    }

    public void awake(long delay) {
        bblock.lock();
        try {
            if (dataflow.isCompleted()) {
                return;
            }
        } finally {
            bblock.unlock();
        }
        TimerTask task = new TimerTask(){
            @Override
            public void run() {
                awake();
            }
        };
        dataflow.getTimer().schedule(task, delay);
    }

    /**
     * finishes parent activity normally.
     */
    public void stop() {
        bblock.lock();
        try {
            dataflow.leave();
        } finally {
            bblock.unlock();
        }
    }

    /**
     * finishes parent activity exceptionally.
     * @param ex the exception
     */
    protected void stop(Throwable ex) {
        bblock.lock();
        try {
            dataflow.onError(ex);
        } finally {
            bblock.unlock();
        }
    }

    public void setExecutor(Executor exec) {
        bblock.lock();
        try {
            this.executor = exec;
        } finally {
            bblock.unlock();
        }
    }

    protected Executor getExecutor() {
        bblock.lock();
        try {
            if (executor == null) {
                executor = dataflow.getExecutor();
            }
            return executor;
        } finally {
            bblock.unlock();
        }
    }

    /**
     * invoked when all asyncTask asyncTask are ready,
     * and method run() is to be invoked.
     * Safe way is to submit this instance as a Runnable to an Executor.
     * Fast way is to invoke it directly, but make sure the chain of
     * direct invocations is short to avoid stack overflow.
     */
    protected void fire() {
        getExecutor().execute(this::run);
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
        /** locking order is: {@link #plock} 1st, {@link #bblock} 2nd */
        protected final Lock plock = new ReentrantLock();
        private boolean ready;
        private Port next;

        public Port(boolean ready) {
            this.ready = ready;
            if (!ready) {
                blockingPortCount++;
                dbg("#incBlocking: blockingPortCount set to ");
            }
            next = ports;
            ports = this;
        }

        public boolean isReady() {
            plock.lock();
            try {
                return ready;
            } finally {
                plock.unlock();
            }
        }

        private final void dbg(String s) {
//            System.out.println(BasicBlock.this.getClass().getName()+"/"+getClass().getSimpleName()+ s +blockingPortCount);
        }

        /**
         * sets this port to a blocked state.
         */
        public synchronized void block() {
            plock.lock();
            try {
                if (!ready) {
                    return;
                }
                ready = false;
                blockingPortCount++;
                dbg("#incBlocking: blockingPortCount set to ");
            } finally {
                plock.unlock();
            }
        }

        /**
         * sets this port to unblocked state.
         * If all ports become unblocked,
         * this block is submitted to the executor.
         *
         * @return value of exptression {@link #blockingPortCount} > 0
         */
        protected void unblock() {
            plock.lock();
            try {
                if (ready) {
                    return;
                }
                ready = true;
                bblock.lock();
                try {
                    if (blockingPortCount == 0) {
                        throw new IllegalStateException("blocked port and blockingPortCount == 0");
                    }
                    blockingPortCount--;
                    dbg("#decBlocking: blockingPortCount set to ");
                    if (blockingPortCount > 0) {
                        return;
                    }
                } finally {
                    bblock.unlock();
                }
            } finally {
                plock.unlock();
            }
            controlport.block();
            fire();
        }

        @Override
        public String toString() {
            return ready?"ready":"blocked";
        }
    }

    private class ControlPort extends Port {
        public ControlPort() {
            super(false);
        }
    }
}