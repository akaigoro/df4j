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
import org.jetbrains.annotations.NotNull;

import java.util.Timer;
import java.util.concurrent.*;

/**
 * A node in dataflow graph.
 * This is a basic class for both AsyncProc and ActorGroup
 * @param <T>
 */
public abstract class Node<T extends Node<T>> implements Activity {
    protected Completion completion = createCompletion();
    protected final ActorGroup actorGroup; // parent node
    private Executor executor;

    protected Node() {
        this.actorGroup = null;
    }

    protected Node(ActorGroup actorGroup) {
        this.actorGroup = actorGroup;
        actorGroup.enter(this);
    }

    @NotNull
    protected Completion createCompletion() {
        return new Completion();
    }

    @Override
    public Throwable getCompletionException() {
        return completion.getCompletionException();
    }

    protected void complete(Throwable ex) {
        whenComplete(ex);
        completion.complete(ex);
        leaveParent(ex);
    }

    protected void complete() {
        complete(null);
    }

    protected void whenComplete(Throwable e) {
    }

    public ActorGroup getActorGroup() {
        return actorGroup;
    }

    protected void leaveParent(Throwable ex) {
        if (actorGroup != null) {
            actorGroup.leave(this, ex);
        }
    }

    public synchronized void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public synchronized Executor getExecutor() {
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

    private static Timer singletonTimer;

    @NotNull
    public static Timer getTimer() {
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

    public synchronized boolean isCompleted() {
        return completion.isCompleted();
    }

    public boolean isCompletedExceptionally() {
        return completion.isCompletedExceptionally();
    }

    @Override
    public boolean isAlive() {
        return !completion.isCompleted();
    }
    @Override
    public void await() throws InterruptedException {
        completion.await();
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return completion.await(timeout, unit);
    }
}