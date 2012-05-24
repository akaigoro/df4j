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
 * An Executor which serves single Actor
 */
public class PrivateExecutor extends Thread implements Executor {
    Executor defaultExecutor;
    Runnable command;
    private boolean completed=false;
    
    /**
     * @param executor
     */
    public PrivateExecutor(Executor defaultExecutor) {
        this.defaultExecutor=defaultExecutor;
        setDaemon(true);
        start();
    }

    public PrivateExecutor() {
        this(Task.getCurrentExecutor());
    }

    @Override
    public synchronized void execute(Runnable command) {
        if (this.command!=null) {
            throw new IllegalStateException("Previous command not finished yet");
        }
        this.command=command;
        notify();
    }

    @Override
    public void run() {
        Task.setCurrentExecutor(defaultExecutor);
        for (;;) {
            Runnable command=null;
            synchronized (this) {
                if (completed) return;
                while (this.command==null) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }
                }
                command=this.command;
                this.command=null;
            }
            command.run();
        }
    }

}
