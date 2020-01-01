package org.df4j.nio2.net;

import org.df4j.core.communicator.ScalarResult;
import org.df4j.core.dataflow.AsyncProc;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.port.OutFlow;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * generates single AsynchronousSocketChannel for client side
 * as a result of the client connection accepted by a server
 */
public class AsyncClientSocketChannel extends ScalarResult<AsynchronousSocketChannel>
        implements CompletionHandler<Void,AsynchronousSocketChannel>
{
    /**
     * Starts connection to a server.
     *
     * @param addr address of the server to connect
     * @throws IOException exception thrown by {@link AsynchronousSocketChannel#open}
     */
    public AsyncClientSocketChannel(SocketAddress addr) throws IOException {
        AsynchronousSocketChannel channel =	AsynchronousSocketChannel.open();
        channel.connect(addr, channel, this);
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
