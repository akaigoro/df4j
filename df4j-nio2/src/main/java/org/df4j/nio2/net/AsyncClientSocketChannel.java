package org.df4j.nio2.net;

import org.df4j.core.dataflow.AsyncProc;
import org.df4j.core.port.CompletablePort;
import org.df4j.core.port.InpScalar;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutorService;

/**
 * Client connection implemented as scalar input port.
 *
 */
public class AsyncClientSocketChannel extends InpScalar<AsynchronousSocketChannel>
        implements CompletionHandler<Void,AsynchronousSocketChannel>
{
    /**
     * @param parent {@link AsyncProc} to which this port belongs
     */
    public AsyncClientSocketChannel(AsyncProc parent) {
        super(parent);
    }

    /**
     * Starts client's connection to a server.
     *
     * @param addr address of the server to connect
     * @throws IOException exception thrown by {@link AsynchronousSocketChannel#open}
     */
    public void connect(SocketAddress addr) throws IOException {
        ExecutorService executor = transition.getExecutor();
        AsynchronousChannelGroup group = AsynchronousChannelGroup.withThreadPool(executor);
        AsynchronousSocketChannel channel =	AsynchronousSocketChannel.open(group);
        channel.connect(addr, channel, this);
    }

    //=====================  CompletionHandler callbacks

    @Override
    public void completed(Void result, AsynchronousSocketChannel channel) {
        onSuccess(channel);
    }

    @Override
    public void failed(Throwable exc, AsynchronousSocketChannel channel) {
        onError(exc);
    }
}

