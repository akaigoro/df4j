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
import org.df4j.core.util.linked.Link;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Timer;
import java.util.concurrent.*;

public abstract class Node<T> extends Completion implements Link<T> {
    private Link<T> prev = this;
    private Link<T> next = this;
    private final Dataflow parent;
    private ExecutorService executor;
    private Timer timer;

    protected Node() {
        this.parent = null;
    }

    protected Node(Dataflow parent) {
        this.parent = parent;
        parent.enter(this);
    }

    @Override
    public Link<T> getNext() {
        return next;
    }

    @Override
    public void setNext(Link<T> next) {
        this.next = next;
    }

    @Override
    public Link<T> getPrev() {
        return prev;
    }

    @Override
    public void setPrev(Link<T> prev) {
        this.prev = prev;
    }

    public Dataflow getParent() {
        return parent;
    }

    protected void leaveParent() {
        if (parent != null) {
            parent.leave(this);
        }
    }

    public void setExecutor(ExecutorService executor) {
        bblock.lock();
        try {
            this.executor = executor;
        } finally {
            bblock.unlock();
        }
    }

    public void setExecutor(Executor executor) {
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
        setExecutor(service);
    }

    public ExecutorService getExecutor() {
        bblock.lock();
        try {
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
        } finally {
            bblock.unlock();
        }
    }

    public void setTimer(Timer timer) {
        bblock.lock();
        try {
            this.timer = timer;
        } finally {
            bblock.unlock();
        }
    }

    public Timer getTimer() {
        bblock.lock();
        try {
            if (timer != null) {
                return timer;
            } else if (parent != null) {
                return timer = parent.getTimer();
            } else {
                return timer = getSingletonTimer();
            }
        } finally {
            bblock.unlock();
        }
    }

    @Override
    public void onComplete() {
        super.onComplete();
        if (parent != null) {
            parent.leave(this);
        }
    }

    protected void onError(Throwable t) {
        super.onError(t);
        if (parent != null) {
            parent.onError(t);
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
}