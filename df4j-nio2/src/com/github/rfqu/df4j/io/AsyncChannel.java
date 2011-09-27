package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ExecutorService;

public class AsyncChannel {
    protected static AsynchronousChannelGroup acg=null;
    
    protected synchronized static AsynchronousChannelGroup getGroup(ExecutorService executor) throws IOException {
        if (acg==null) {
            acg=AsynchronousChannelGroup.withThreadPool(executor);
        }
        return acg;
    }
}
