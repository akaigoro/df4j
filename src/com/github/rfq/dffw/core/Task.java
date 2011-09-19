/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfq.dffw.core;

import java.util.concurrent.Executor;


public abstract class Task extends Link implements Runnable {
    /**
     * activates this task by sending it to the current executor
     */
    protected Task fire() {
        Executor currentExecutor = Actor.getCurrentExecutor();
        if (currentExecutor==null) {
            throw new IllegalStateException("current executor is not set)");
        }
        currentExecutor.execute(this);
        return this;
    }
}