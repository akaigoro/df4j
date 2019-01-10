package org.df4j.nio2.net.echo;

import org.df4j.core.boundconnector.permitstream.Semafor;
import org.df4j.core.tasknode.Action;
import org.df4j.core.tasknode.AsyncAction;
import org.df4j.nio2.net.AsyncServerSocketChannel;
import org.df4j.nio2.net.ServerConnection;

import java.io.IOException;
import java.net.SocketAddress;

/**
 * generates {@link ServerConnection}s and passes them to AsyncServerSocketChannel to initialize
 *
 */
public class ConnectionManager extends AsyncAction {
    private final AsyncServerSocketChannel assc;
    Semafor allowedConnections = new Semafor(this);
    int serialnum=0;

    Subscriber<ServerConnection> backport = (asyncSocketChannel) -> {
        allowedConnections.release();
    };

    public ConnectionManager(SocketAddress addr, int connCount) throws IOException {
        assc = new AsyncServerSocketChannel(addr);
        allowedConnections.release(connCount);
    }

    @Action
    protected void act() {
        ServerConnection conn = new EchoServer(backport);
        conn.name = "EchoServerConnection"+(serialnum++);
        assc.subscribe(conn);
    }

    public void close() {
        assc.close();
        stop();
    }
}
