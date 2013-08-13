/*
 * Copyright 2013 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.ext;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executor;

import com.github.rfqu.df4j.core.Task;

/** 
 * Independent implementation of a Serial Executor.
 */
public class SecondaryExecutor implements Executor {
    /** rest of tokens */
    private final Queue<Runnable> queue = new LinkedList<Runnable>();
    /** in order not to expose run() method */ 
    private final MyTask myTask;

    public SecondaryExecutor() {
        myTask=new MyTask();
    }

    public SecondaryExecutor(Executor executor) {
        myTask=new MyTask(executor);
    }

    /** 
     * Frontend method which may be called from other Thread or Actor.
     * Saves the message and initiates Actor's execution.
     */
    public final void execute(Runnable message) {
        if (message==null) {
            throw new IllegalArgumentException("message may not be null"); 
        }
        synchronized(queue) {
            if (myTask.message != null) {
                queue.add(message);
                return;
            }
            myTask.message=message;
        }
        myTask.fire();
    }

    private final class MyTask extends Task {
        /** current token */
        private Runnable message=null;

        public MyTask() {
        }

        public MyTask(Executor executor) {
            super(executor);
        }

        @Override
        public final void run() {
            for (;;) {
                message.run();
                synchronized(queue) {
                    if ((message = queue.poll())==null) {
                        return;
                    }
                }
            }
        }
    }
}
