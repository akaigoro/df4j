package org.df4j.nio2.net;

import org.df4j.core.communicator.ScalarResult;
import org.df4j.core.dataflow.Dataflow;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutorService;

/**
 * generates single AsynchronousSocketChannel for client side
 * as a result of the client connection accepted by a server
 */
public class AsyncClientSocketChannel extends ScalarResult<AsynchronousSocketChannel>
        implements CompletionHandler<Void,AsynchronousSocketChannel>
{
    private Dataflow dataflow;
    /**
     * Starts connection to a server.
     *
     *
     *
     * @param executor
     * @param addr address of the server to connect
     * @throws IOException exception thrown by {@link AsynchronousSocketChannel#open}
     */
    public AsyncClientSocketChannel(ExecutorService executor, SocketAddress addr) throws IOException {
        AsynchronousChannelGroup group = AsynchronousChannelGroup.withThreadPool(executor);
        AsynchronousSocketChannel channel =	AsynchronousSocketChannel.open(group);
        channel.connect(addr, channel, this);
    }

    public AsyncClientSocketChannel(Dataflow dataflow, SocketAddress addr) throws IOException {
        this(dataflow.getExecutor(), addr);
    }

    //=====================  CompletionHandler callbacks

    @Override
    public void completed(Void result, AsynchronousSocketChannel channel) {
        super.onSuccess(channel);
    }

    @Override
    public void failed(Throwable exc, AsynchronousSocketChannel channel) {
        super.onError(exc);
    }
}
