package com.github.rfqu.df4j.nio.echo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.rfqu.df4j.core.ListenableFuture;
import com.github.rfqu.df4j.core.Port;
import com.github.rfqu.df4j.nio.AsyncServerSocketChannel;
import com.github.rfqu.df4j.nio.AsyncSocketChannel;
import com.github.rfqu.df4j.nio.LimitedServer;

/** Running this echo server requires that an implementation of {@link com.github.rfqu.df4j.nio.AsyncChannelFactory}
* be present in the classpath {@see com.github.rfqu.df4j.nio.AsyncChannelFactory#factoryClassNames}.
* The easiest way to provide this is to run extention classes from df4j-nio1
* or df4j-nio2.
* 
* To run tests, {@see EchoServerLockTest} and {@see EchoServerGlobTest}.
*/
public class EchoServer extends LimitedServer         
    implements Port<AsyncSocketChannel>
{
    public static final int defaultPort = 8007;
    public static final int BUF_SIZE = 128;

    final AtomicInteger ids = new AtomicInteger(); // for debugging
    /** active connections */
    final HashMap<Integer, ServerConnection> connections = new HashMap<Integer, ServerConnection>();

    public EchoServer(SocketAddress addr) throws IOException {
        super(addr);
    }

    protected void start(int maxConnCount) {
        super.start(this, 1, maxConnCount);
    }

    public ListenableFuture<AsyncServerSocketChannel> getCloseEvent() {
        return assc.getCloseEvent();
    }

    protected synchronized void connClosed(ServerConnection serverConnection) {
        if (assc.isClosed()) {
            return;
        }
        connections.remove(serverConnection.id);
    }

    @Override
    public synchronized void close() {
        if (assc.isClosed()) {
            return;
        }
        assc.close();
        Iterator<Integer> ids = connections.keySet().iterator();
        while (ids.hasNext()) {
            Integer id = ids.next();
            connections.get(id).close();
            ids.remove();
        }
    }

    // ==================== actor's backend

    /**
     * AsyncServerSocketChannel informs on new client connection
     */

	@Override
    public void post(AsyncSocketChannel channel) {
        ServerConnection connection = new ServerConnection(EchoServer.this, channel);
        connections.put(connection.id, connection);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("classPath=" + System.getProperty("java.class.path"));
        Integer port;
        if (args.length >= 1) {
            port = Integer.valueOf(args[0]);
        } else {
            port = defaultPort;
        }
        int maxConn;
        if (args.length >= 2) {
            maxConn = Integer.valueOf(args[1]);
        } else {
            maxConn = 1000;
        }
        SocketAddress addr = new InetSocketAddress("localhost", port);
		EchoServer es = new EchoServer(addr);
		es.start(maxConn);
        es.getCloseEvent().get();
        // inet addr is free now
        System.out.println("EchoServer started");
    }
}
