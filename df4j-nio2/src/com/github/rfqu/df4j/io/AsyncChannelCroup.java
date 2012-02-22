package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ExecutorService;

import com.github.rfqu.df4j.core.Task;

public class AsyncChannelCroup {
    /**
     * sets current group as a thread-local variable
     * @param exec
     */
    public static void setCurrentACGroup(AsynchronousChannelGroup group) {
        currentACGroup.set(group);
    }

    /**
     * retrieves current group as a thread-local variable
     */
    public static AsynchronousChannelGroup getCurrentACGroup() {
        return currentACGroup.get();
    }

    private static final ThreadLocal <AsynchronousChannelGroup> currentACGroup 
        = new ThreadLocal <AsynchronousChannelGroup> () {

            @Override
            protected AsynchronousChannelGroup initialValue() {
                ExecutorService executor=Task.getCurrentExecutor();

                try {
                    return AsynchronousChannelGroup.withThreadPool(executor);
                } catch (IOException e) {
                    return null;
                }
            }
        
    };
    
}
