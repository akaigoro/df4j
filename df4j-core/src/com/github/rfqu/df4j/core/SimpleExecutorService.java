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

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/** 
 * incomplete
 *
 */
public class SimpleExecutorService extends AbstractExecutorService {
    Worker worker = new Worker();

    public SimpleExecutorService() {
        worker.start();
    }

    /**
     * Simple single-threaded context executor.
     * It is also an Actor accepting Tasks.
     * @param <Task> the type of accepted tasks
     */
    class Worker extends Link implements Port<Task>, Runnable, Executor {
        protected MessageQueue<Task> input=new MessageQueue<Task>();
        protected boolean ready=true;
        protected boolean running;
        volatile boolean _stop=false;
        protected Thread t=new Thread(this);
        
        /**
         * default constructor
         */
        public Worker() {
            t.setDaemon(true);
            t.setName(t.getName()+" DF executor");
        }

        /**
         * starts the underlying thread
         */
        public void start() {
            running = true;
            t.start();
        }

        /**
         * Executes the given command at some time in the future. The command executes in the underlying thread.
         * Parameters:
         * command the runnable task
         * Throws:
         * RejectedExecutionException if this task cannot be accepted for execution.
         * NullPointerException if command is null 
         */
        @Override
        public void execute(Runnable command) {
            if (_stop) {
                throw new RejectedExecutionException();
            }
            if (command==null) {
                throw new NullPointerException();
            } else if (command instanceof Task) {
                send((Task) command);
            } else {
                send(new TaskWrapper(command));
            }
        }

        /**
         * Initiates an orderly shutdown in which previously submitted tasks are executed, 
         * but no new tasks will be accepted.
         * Invocation has no additional effect if already shut down. 
         */
        public synchronized void shutdown() {
            _stop=true;
            notifyAll();
        }

        /**
         * the receiving endpoint for tasks.
         * @return 
         */
        @Override
        public synchronized Worker send(Task task) {
            input.enqueue(task);
            if (ready && !running) {
                running=true;
                notifyAll();
            }
            return this;
        }

        /**
         * executes enqueued tasks sequentially.
         */
        @Override
        public void run() {
            Task.setCurrentExecutor(SimpleExecutorService.this);
            for (;;) {
                Task task;
                synchronized (this) {
                    for (;;) {
                        if (_stop) {
                            return;
                        }
                        task = input.poll();
                        if (task != null) {
                            break;
                        }
                        running=false;
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                }
                try {
                    task.run();
                } catch (Exception e) {
                    failure(task, e);
                }
            }
        }

        /** handles the failure
         * 
         * @param message
         * @param e
         */
        protected void failure(Task message, Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public boolean isShutdown() {
        return worker._stop;
    }

    @Override
    public boolean isTerminated() {
        return worker._stop;
    }

    @Override
    public synchronized void shutdown() {
        worker.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        worker.shutdown();
        return null; // TODO
    }

    @Override
    public void execute(Runnable command) {
        worker.execute(command);
    }

}
