/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An actor is like a Petri Net trasnsition with own places for tokens.
 * Shared places cannot be represented directly. To some extent, the role
 * of shared places is played by {@link org.df4j.core.ext.Dispatcher}
 *
 * Own places can be of 2 sorts: carrying colorless tokens (without information,
 * like Starter and Semafor, and and carrying colored tokens, which are references to arbitrary objects.
 *
 * Actor is started when all its places are not empty (contain tokens). Excecution means execution
 * its (@link {@link Actor#act()} method on the executor set by {@link #setExecutor} method.
 */
public abstract class Actor extends BaseActor implements Runnable {

    protected final AtomicReference<Executor> executor = new AtomicReference<>();

    /**
     * assigns Executor
     * returns previous executor
     */
    public Executor setExecutor(Executor exec) {
        Executor res = this.executor.getAndUpdate((prev)->exec);
        return res;
    }

    protected Executor getExecutor() {
        Executor exec = executor.get();
        return exec;
    }

    protected Executor getExecutorNotNull() {
        Executor exec = executor.get();
        if (exec == null) {
            exec = executor.updateAndGet((prev)->prev==null?ForkJoinPool.commonPool():prev);
        }
        return exec;
    }

    /**
     * invoked when all transition transition are ready,
     * and method run() is to be invoked.
     * Safe way is to submit this instance as a Runnable to an Executor.
     * Fast way is to invoke it directly, but make sure the chain of
     * direct invocations is limited to avoid stack overflow.
     */
    protected void fire() {
        Executor executor = getExecutorNotNull();
        executor.execute(this);
    }

    @Override
    public void run() {
        try {
            act();
            transition.consumeTokens();
            controlPin.turnOnAndFire();
        } catch (Throwable e) {
            System.err.println("Error in actor " + getClass().getName());
            e.printStackTrace();
        }
    }

    // ========= backend

    /**
     * reads extracted tokens from places and performs specific calculations
     *
     * @throws Exception
     */
    protected abstract void act() throws Exception;
}