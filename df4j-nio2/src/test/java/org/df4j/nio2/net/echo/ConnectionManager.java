package org.df4j.nio2.net.echo;

import org.df4j.core.connector.messagescalar.ScalarCollector;
import org.df4j.core.connector.permitstream.Semafor;
import org.df4j.core.node.Action;
import org.df4j.core.node.AsyncTask;
import org.df4j.nio2.net.AsyncServerSocketChannel;
import org.df4j.nio2.net.AsyncSocketChannel;
import org.df4j.nio2.net.ServerConnection;

import java.io.IOException;
import java.net.SocketAddress;

public class ConnectionManager extends AsyncTask {
    private final AsyncServerSocketChannel assc;
    Semafor allowedConnections = new Semafor(this);
    int serialnum=0;

    ScalarCollector<AsyncSocketChannel> backport = (asyncSocketChannel) -> {
        allowedConnections.release();
    };

    public ConnectionManager(SocketAddress addr, int connCount) throws IOException {
        assc = new AsyncServerSocketChannel(addr);
        allowedConnections.release(connCount);
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
