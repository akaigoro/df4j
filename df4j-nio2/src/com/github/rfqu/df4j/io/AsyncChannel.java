package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ExecutorService;

import com.github.rfqu.df4j.core.Task;

public class AsyncChannel {
    
    protected synchronized static AsynchronousChannelGroup getGroup() throws IOException {
        return currentASGKey.get();
    }

    private static final ThreadLocal <AsynchronousChannelGroup> currentASGKey = new ThreadLocal <AsynchronousChannelGroup> () {

        @Override
        protected AsynchronousChannelGroup initialValue() {
            ExecutorService executor=Task.getCurrentExecutor();
            try {
                return AsynchronousChannelGroup.withThreadPool(executor);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            }
        }
        
    };
}
