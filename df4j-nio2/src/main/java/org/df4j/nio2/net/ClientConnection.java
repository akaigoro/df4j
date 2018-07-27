package org.df4j.nio2.net;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class ClientConnection extends ServerConnection
        implements CompletionHandler<Void,AsynchronousSocketChannel>
{
    /**
     * Starts connection to a server. IO requests can be queued immediately,
     * but will be executed only after connection completes.
     *
     * @throws IOException
     */
    public ClientConnection(String name, SocketAddress addr) throws IOException, InterruptedException {
        super(name);
        AsynchronousSocketChannel channel =	AsynchronousSocketChannel.open();
        channel.connect(addr, channel, this);
    }

    //=====================  CompletionHandler callbacks

    @Override
    public void completed(Void result, AsynchronousSocketChannel channel) {
        super.complete(channel);
    }

    @Override
    public void failed(Throwable exc, AsynchronousSocketChannel channel) {
        super.completeExceptionally(exc);
    }

}
