package com.github.rfqu.df4j.nio.echo;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.rfqu.df4j.core.ActorVariable;
import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.CompletableFuture;
import com.github.rfqu.df4j.nio.AsyncChannelFactory;
import com.github.rfqu.df4j.nio.AsyncServerSocketChannel;
import com.github.rfqu.df4j.nio.AsyncSocketChannel;

/** Running this echo server requires that an implementation of {@link com.github.rfqu.df4j.nio.AsyncChannelFactory}
* be present in the classpath {@see com.github.rfqu.df4j.nio.AsyncChannelFactory#factoryClassNames}.
* The easiest way to provide this is to run extention classes from df4j-nio1
* or df4j-nio2.
* 
* To run tests, {@see EchoServerLockTest} and {@see EchoServerGlobTest}.
*/
public class EchoServer
    extends ActorVariable<AsyncSocketChannel> // as a connection acceptor
    implements Closeable
{
    public static final int defaultPort = 8007;
    public static final int BUF_SIZE = 128;

    AsyncChannelFactory asyncChannelFactory=AsyncChannelFactory.getCurrentAsyncChannelFactory();
    AtomicInteger ids = new AtomicInteger(); // for DEBUG
    SocketAddress addr; // address of this server
    /** maximum allowed numer of simultaneous connections */
    int maxConn;
    AsyncServerSocketChannel assch;  // provides ready connections for us
    /** active connections */
    HashMap<Integer, ServerConnection> connections = new HashMap<Integer, ServerConnection>();

    public EchoServer(SocketAddress addr, int maxConn) throws IOException {
        this.addr = addr;
        this.maxConn = maxConn;
        assch = asyncChannelFactory.newAsyncServerSocketChannel(addr);
        assch.post(this);  // ready to accept first clien connection request
    }

    public CompletableFuture<SocketAddress> getCloseEvent() {
        return assch.getCloseEvent();
    }

    protected synchronized void connClosed(ServerConnection serverConnection) {
        if (assch.isClosed()) {
            return;
        }
        connections.remove(serverConnection.id);
        // System.out.println("connections="+connections.size());
        assch.post(this); // allow next accept
    }

    @Override
    public synchronized void close() {
        if (assch.isClosed()) {
            return;
        }
        assch.close();
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
    protected synchronized void act(AsyncSocketChannel channel) throws Exception {
        ServerConnection connection = new ServerConnection(EchoServer.this, channel);
        connections.put(connection.id, connection);
        if (connections.size() < maxConn) {
            assch.post(this); // allow next accept
        }
    }

    /**
     * AsyncServerSocketChannel sends failure
     */
    @Override
    public void postFailure(Throwable exc) {
        exc.printStackTrace();
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
        EchoServer es = new EchoServer(addr, maxConn);
        es.getCloseEvent().get();
        // inet addr is free now
        System.out.println("EchoServer started");
    }
}
