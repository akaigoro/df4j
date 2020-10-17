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

import org.df4j.core.connector.Completion;
import org.df4j.core.util.linked.LinkImpl;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Timer;
import java.util.concurrent.*;

public abstract class Node<T extends Node<T>> extends Completion {
    public final long seqNum;
    NodeLink nodeLink = new NodeLink();
    private final Dataflow parent;
    private Executor executor;
    private ExecutorService executorService;
    private Timer timer;

    protected Node() {
        this.parent = null;
        seqNum = -1;
    }

    protected Node(Dataflow parent) {
        this.parent = parent;
        seqNum = parent.enter(this);
    }

    public Dataflow getParent() {
        return parent;
    }

    protected void leaveParent() {
        if (parent != null) {
            parent.leave(this);
        }
    }

    public synchronized void setExecutor(ExecutorService executor) {
        this.executor = executor;
        this.executorService = executor;
    }

    public synchronized void setExecutor(Executor executor) {
        this.executor = executor;
        this.executorService = null;
    }

    public synchronized Executor getExecutor() {
        if (executor == null) {
            if (parent != null) {
                executor = parent.getExecutor();
            } else {
                Thread currentThread = Thread.currentThread();
                if (currentThread instanceof ForkJoinWorkerThread) {
                    executor = ((ForkJoinWorkerThread) currentThread).getPool();
                } else {
                    executor = ForkJoinPool.commonPool();
                }
            }
        }
        return executor;
    }

    public synchronized ExecutorService getExecutorService() {
        if (executorService != null) {
            return executorService;
        }
        Executor executor = this.executor;
        if (executor instanceof ExecutorService) {
            this.executorService = (ExecutorService) executor;
            return (ExecutorService) executor;
        }
        ExecutorService service = new AbstractExecutorService(){
            @Override
            public void execute(@NotNull Runnable command) {
                executor.execute(command);
            }

            @Override
            public void shutdown() {

            }

            @NotNull
            @Override
            public List<Runnable> shutdownNow() {
                return null;
            }

            @Override
            public boolean isShutdown() {
                return false;
            }

            @Override
            public boolean isTerminated() {
                return false;
            }

            @Override
            public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
                return false;
            }
        };
        this.executorService = service;
        return service;
    }

    public void setTimer(Timer timer) {
        synchronized(this) {
            this.timer = timer;
        }
    }

    public Timer getTimer() {
        synchronized(this) {
            if (timer != null) {
                return timer;
            } else if (parent != null) {
                return timer = parent.getTimer();
            } else {
                return timer = getSingletonTimer();
            }
        }
    }

    @Override
    protected void complete() {
        super.complete();
        if (parent != null) {
            parent.leave(this);
        }
    }

    protected void completeExceptionally(Throwable t) {
        super.completeExceptionally(t);
        if (parent != null) {
            parent.completeExceptionally(t);
        }
    }

    private static Timer singletonTimer;

    @NotNull
    public static Timer getSingletonTimer() {
        Timer res = singletonTimer;
        if (res == null) {
            synchronized (Dataflow.class) {
                res = singletonTimer;
                if (res == null) {
                    res = singletonTimer = new Timer();
                }
            }
        }
        return res;
    }

    @Override
    public String toString() {
        return "(#"+seqNum+')'+super.toString();
    }

    class NodeLink extends LinkImpl {

        public T getItem() {
            return (T) Node.this;
        }

        @Override
        public String toString() {
            return getItem().toString();
        }
    }
}