/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.ext;

import java.util.concurrent.Executor;
import java.awt.EventQueue;

import org.df4j.core.*;

/**
 * Processes task on the Event Dispatch Thread.
 */
public class SwingExecutor implements Executor {
    
    /** sets DF context (including executor) for tasks created on the swing EDT.
     */
    public static void invokeFirstTask(final Runnable firstTask, final DFContext context) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                DFContext.setCurrentContext(context);
                firstTask.run();
            }
        });
    }
    
    /** sets current DF context as context (including executor) for tasks created on the swing EDT.
     */
    public static void invokeFirstTask(final Runnable firstTask) {
        invokeFirstTask(firstTask, DFContext.getCurrentContext());
    }

    @Override
    public void execute(Runnable command) {
        EventQueue.invokeLater(command);
    }

}
