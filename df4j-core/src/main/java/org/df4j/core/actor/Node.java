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

import org.df4j.core.connector.Completion;
import org.df4j.core.util.linked.LinkImpl;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Timer;
import java.util.concurrent.*;

public abstract class Node<T extends Node<T>> extends Completion implements Activity {
    public final long seqNum;
    NodeLink nodeLink = new NodeLink();
    protected final ActorGroup actorGroup;
    private ExecutorService executor;
    private Timer timer;

    protected Node() {
        this.actorGroup = null;
        seqNum = -1;
    }

    protected Node(ActorGroup actorGroup) {
        this.actorGroup = actorGroup;
        seqNum = actorGroup.enter(this);
    }

    public ActorGroup getDataflow() {
        return actorGroup;
    }

    protected void leaveParent() {
        if (actorGroup != null) {
            actorGroup.leave(this);
        }
    }

    public void setExecutor(ExecutorService executor) {
        synchronized(this) {
            this.executor = executor;
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
            public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) {
                return false;
            }
        };
        setExecutor(service);
    }

    public synchronized ExecutorService getExecutor() {
        if (executor == null) {
            if (actorGroup != null) {
                executor = actorGroup.getExecutor();
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

    public void setTimer(Timer timer) {
        synchronized(this) {
            this.timer = timer;
        }
    }

    public Timer getTimer() {
        synchronized(this) {
            if (timer != null) {
                return timer;
            } else if (actorGroup != null) {
                return timer = actorGroup.getTimer();
            } else {
                return timer = getSingletonTimer();
            }
        }
    }

    @Override
    public void complete() {
        super.complete();
        if (actorGroup != null) {
            actorGroup.leave(this);
        }
    }

    public void completeExceptionally(Throwable t) {
        super.completeExceptionally(t);
        if (actorGroup != null) {
            actorGroup.completeExceptionally(t);
        }
    }

    private static Timer singletonTimer;

    @NotNull
    public static Timer getSingletonTimer() {
        Timer res = singletonTimer;
        if (res == null) {
            synchronized (ActorGroup.class) {
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