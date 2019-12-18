package org.df4j.nio2.net;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class ClientConnection extends BaseConnection
        implements CompletionHandler<Void,AsynchronousSocketChannel>
{
    /**
     * Starts connection to a server. IO requests can be queued immediately,
     * but will be executed only after connection completes.
     *
     * @param name name of the connection
     * @param addr address of the server to connect
     * @throws IOException exception thrown by {@link AsynchronousSocketChannel#open}
     */
    public ClientConnection(String name, SocketAddress addr) throws IOException {
        super(name, null);
        AsynchronousSocketChannel channel =	AsynchronousSocketChannel.open();
        channel.connect(addr, channel, this);
    }

    //=====================  CompletionHandler callbacks

    @Override
    public void completed(Void result, AsynchronousSocketChannel channel) {
        super.setChannel(channel);
    }

    @Override
    public void failed(Throwable exc, AsynchronousSocketChannel channel) {
        super.onError(exc);
    }

}
