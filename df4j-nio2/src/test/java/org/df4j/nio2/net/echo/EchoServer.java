package org.df4j.nio2.net.echo;

import org.df4j.nio2.net.AsyncServerSocketChannel;
import org.df4j.nio2.net.ServerConnection;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * generates {@link EchoServerConnection}s for incoming connections
 *
 */
public class EchoServer extends AsyncServerSocketChannel {
    AtomicInteger serialnum=new AtomicInteger(0);

    public EchoServer(SocketAddress addr, int connCount) throws IOException {
        super(addr);
        allowedConnections.release(connCount);
    }

    @Override
    protected void onNext(AsynchronousSocketChannel asc) {
        LOG.finest("AsynchronousServerSocketChannel: request accepted");
        getExecutor().execute(()->{
            ServerConnection conn = new EchoServerConnection(asc, allowedConnections);
            conn.name = "EchoServerConnection"+(serialnum.getAndIncrement());
        });
    }

    @Override
    protected void onError(Throwable exc) {
        LOG.finest("AsynchronousServerSocketChannel: request failed:"+exc);
    }
}
