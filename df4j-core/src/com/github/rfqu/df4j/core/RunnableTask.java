/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.core;

import java.util.concurrent.Executor;

/**
 * Base class for dataflow nodes (including Actors).
 * Features:
 *  - Task is Runnable - can be submitted to an Executor
 *  - Task holds default executor, which was set at the moment of creation:
 *  either explicitly, or implicitly via thread-local current executor.
 *  If executor is null, firing executes immediately on the thread which fires the task.
 *  - Task is Link, and can be enqueued in the standard {link:DoublyLinkedQueue},
 *   e.g can be sent to ActorLQ.
 *   
 * @author kaigorodov
 */
public abstract class RunnableTask implements Runnable, Task {
    protected final Executor executor;

    public RunnableTask(Executor executor) {
        if (executor==null) {
            this.executor = DFContext.getCurrentExecutor();
        } else {
            this.executor = executor;
        }
    }

    /**
     * activates this task by sending it to the executor
     */
    public final void fire() {
        executor.execute(this);
    }

}