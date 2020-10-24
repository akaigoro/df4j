package org.df4j.nio2.net;

import org.df4j.core.actor.AsyncProc;
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
 * Though it is an input port, it does not require connection to other actor.
 * use {@link #connect(SocketAddress)} instead.
 */
public class ClientSocketPort extends InpScalar<AsynchronousSocketChannel> {
    /**
     * @param parent {@link AsyncProc} to which this port belongs
     */
    public ClientSocketPort(AsyncProc parent) {
        super(parent);
    }

    /**
     * Starts client's connection to a server.
     *
     * @param addr address of the server to connect
     * @throws IOException exception thrown by {@link AsynchronousSocketChannel#open}
     */
    public void connect(SocketAddress addr) throws IOException {
        ExecutorService executor = getParentActor().getExecutorService();
        AsynchronousChannelGroup group = AsynchronousChannelGroup.withThreadPool(executor);
        AsynchronousSocketChannel channel =	AsynchronousSocketChannel.open(group);
        CompletionHandler<Void,AsynchronousSocketChannel> handler = new Handler();
        channel.connect(addr, channel, handler);
    }

    public void cancel() throws IOException {
        AsynchronousSocketChannel channel = current();
        if (channel == null) {
            return;
        }
        channel.close();
    }

    private class Handler implements CompletionHandler<Void, AsynchronousSocketChannel> {
        @Override
        public void completed(Void result, AsynchronousSocketChannel channel) {
            onSuccess(channel);
        }

        @Override
        public void failed(Throwable exc, AsynchronousSocketChannel channel) {
            onError(exc);
        }
    }

    //=====================  CompletionHandler callbacks
}

