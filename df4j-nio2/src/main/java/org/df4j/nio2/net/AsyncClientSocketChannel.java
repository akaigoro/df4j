package org.df4j.nio2.net;

import org.df4j.core.actor.AsyncProc;
import org.df4j.core.actor.TransitionHolder;
import org.df4j.core.port.CompletablePort;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

/**
 * Client connection implemented as scalar input port.
 *
 */
public class AsyncClientSocketChannel extends CompletablePort
        implements CompletionHandler<Void,AsynchronousSocketChannel>
{
    private final ExecutorService execService = ForkJoinPool.commonPool();
    protected AsynchronousSocketChannel asc;

    /**
     * @param parent {@link AsyncProc} to which this port belongs
     */
    public AsyncClientSocketChannel(TransitionHolder parent) {
        super(parent);
    }

    /**
     * Starts client's connection to a server.
     *
     * @param addr address of the server to connect
     * @throws IOException exception thrown by {@link AsynchronousSocketChannel#open}
     */
    public void connect(SocketAddress addr) throws IOException {
        AsynchronousChannelGroup group = AsynchronousChannelGroup.withThreadPool(execService);
        AsynchronousSocketChannel channel =	AsynchronousSocketChannel.open(group);
        channel.connect(addr, channel, this);
    }

    public synchronized AsynchronousSocketChannel current() {
        return asc;
    }

    //=====================  CompletionHandler callbacks

    @Override
    public synchronized void completed(Void result, AsynchronousSocketChannel channel) {
        this.asc = channel;
        onComplete();
    }

    @Override
    public void failed(Throwable exc, AsynchronousSocketChannel channel) {
        onError(exc);
    }
}

