package org.df4j.nio2.net.echo;

import org.df4j.core.actor.LazyActor;
import org.df4j.core.actor.Semafor;
import org.df4j.nio2.net.AsyncServerSocketChannel;
import org.df4j.nio2.net.ServerConnection;

import java.io.IOException;
import java.net.SocketAddress;

/**
 * generates {@link ServerConnection}s and passes them to AsyncServerSocketChannel to initialize
 *
 */
public class ConnectionManager extends LazyActor {
    private final AsyncServerSocketChannel assc;
    Semafor allowedConnections = new Semafor(this);
    int serialnum=0;

    void release(ServerConnection asyncSocketChannel) {
        allowedConnections.release();
    }

    public ConnectionManager(SocketAddress addr, int connCount) throws IOException {
        assc = new AsyncServerSocketChannel(addr);
        allowedConnections.release(connCount);
    }
    
    @Override
    protected void runAction() throws Throwable {
        ServerConnection conn = new EchoServer(this::release);
        conn.name = "EchoServerConnection"+(serialnum++);
        assc.subscribe(conn);
    }

    public void close() {
        assc.close();
        stop();
    }
}
