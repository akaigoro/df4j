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
