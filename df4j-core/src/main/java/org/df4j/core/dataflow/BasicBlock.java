/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.dataflow;

import org.df4j.core.communicator.Completion;
import org.reactivestreams.*;
import org.df4j.protocol.SignalFlow;

import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link BasicBlock} is the most primitive component of {@link Dataflow} graph.
 *  It plays the same role as basic blocks in a flow chart
 *  (see <a href="https://en.wikipedia.org/wiki/Flowchart">https://en.wikipedia.org/wiki/Flowchart</a>).
 * {@link BasicBlock} contains single predefined port to accept flow of control by call to the method {@link BasicBlock#awake()}.
 * This embedded port is implicitly switched off when this {@link BasicBlock} is fired,
 * so the method {@link BasicBlock#awake()} must be called again explicitly if next firings are required.
 * Unlike basic blocks in traditional flow charts, different {@link BasicBlock}s in the same {@link Dataflow} graph can run in parallel.
 *
 * {@link BasicBlock} can contain additional input and output ports
 * to exchange messages and signals with ports of other {@link BasicBlock}s in consistent manner.
 * {@link BasicBlock} is submitted for execution to its executor when all ports become ready, including the embedded control port.
 */
public abstract class BasicBlock extends Completion implements SignalFlow.Subscriber {
    protected final Lock bblock = new ReentrantLock();
    protected Dataflow dataflow;
    /** is not encountered as a parent's child */
    private boolean daemon;
    /**
     * blocked initially, until {@link #awake} called.
     */
    private Port ports = null;
    private int blockingPortCount = 0;
    private Executor executor;
    private Port controlport = new ControlPort();
    private Subscription subscription;

    protected BasicBlock(Dataflow dataflow) {
        if (dataflow == null) {
            throw new IllegalArgumentException();
        }
        this.dataflow = dataflow;
        dataflow.enter();
    }

    public Dataflow getDataflow() {
        return dataflow;
    }

    public void setDaemon(boolean daemon) {
        bblock.lock();
        try {
            if (this.daemon) {
                return;
            }
            this.daemon = daemon;
            if (dataflow != null) {
                dataflow.leave();
            }
        } finally {
            bblock.unlock();
        }
    }

    public boolean isDaemon() {
        bblock.lock();
        try {
            return daemon;
        } finally {
            bblock.unlock();
        }
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        bblock.lock();
        try {
            if (this.subscription != null) {
                this.subscription.cancel();
            }
            this.subscription = subscription;
            subscription.request(1);
        } finally {
            bblock.unlock();
        }
    }

    public void unsubscribe() {
        bblock.lock();
        try {
            if (this.subscription == null) {
                return;
            }
            this.subscription.cancel();
            this.subscription = null;
        } finally {
            bblock.unlock();
        }
    }

        /**
         * passes a control token to this {@link BasicBlock}.
         * This token is consumed when this block is submitted to an executor.
         */
    public void awake() {
        bblock.lock();
        try {
            if (isCompleted()) {
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
            if (isCompleted()) {
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
            if (isCompleted()) {
                return;
            }
            super.onComplete();
            if (dataflow != null) {
                dataflow.leave();
            }
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
            if (isCompleted()) {
                return;
            }
            onError(ex);
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

    public Executor getExecutor() {
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
                bblock.lock();
                try {
                    blockingPortCount++;
                } finally {
                    bblock.unlock();
                }
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
                bblock.lock();
                try {
                    blockingPortCount++;
                } finally {
                    bblock.unlock();
                }
            } finally {
                plock.unlock();
            }
        }

        /**
         * sets this port to unblocked state.
         * If all ports become unblocked,
         * this block is submitted to the executor.
         */
        public void unblock() {
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
       //             dbg("#unblock: blockingPortCount = "+blockingPortCount);
                    if (blockingPortCount > 0) {
                        return;
                    }
                    if (isCompleted()) {
                        throw new IllegalStateException("Zombie Apocalypse");
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

        protected Dataflow getDataflow() {
            return dataflow;
        }
    }

    private class ControlPort extends Port {
        public ControlPort() {
            super(false);
        }

        @Override
        public synchronized void block() {
            super.block();
        }

        @Override
        public void unblock() {
            super.unblock();
        }
    }
}