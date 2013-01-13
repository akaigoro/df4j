package com.github.rfqu.df4j.nio2.echo;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.rfqu.df4j.core.ActorVariable;
import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.CallbackFuture;
import com.github.rfqu.df4j.nio.AsyncServerSocketChannel;
import com.github.rfqu.df4j.nio.AsyncSocketChannel;
import com.github.rfqu.df4j.test.AsyncChannelFactory;

public class EchoServer extends ActorVariable<SocketChannel>
    implements Closeable
{
	public static final int defaultPort = 9993;
    public static final int BUF_SIZE = 128;

    AsyncChannelFactory asyncChannelFactory;
	AtomicInteger ids=new AtomicInteger(); // for DEBUG    
    SocketAddress addr;
    AsyncServerSocketChannel assch;
    /** active connections */
    HashMap<Integer, ServerConnection> connections=new HashMap<Integer, ServerConnection>();
    /** listeners to the closing event */
        
    public EchoServer(AsyncChannelFactory asyncChannelFactory, SocketAddress addr, int maxConn) throws IOException {
        this.asyncChannelFactory=asyncChannelFactory;
        this.addr=addr;
        assch=asyncChannelFactory.newAsyncServerSocketChannel(addr, this);
        assch.up(maxConn);
    }

    public <R extends Callback<SocketAddress>> R addCloseListener(R listener) {
    	return assch.addCloseListener(listener);
    }

    protected synchronized void connClosed(ServerConnection connection) {
        connections.remove(connection.id);
        if (assch.isClosed()) {
            return;
        }
        //            System.out.println("connections="+connections.size());
        assch.up(); // allow next accept
    }

//    @Override
    public synchronized void close() {
        if (assch.isClosed()) {
            return;
        }
        assch.close();
        for (ServerConnection connection: connections.values()) {
        	connection.close();
        }
    }

    //==================== actor's backend

    /** AsyncServerSocketChannel sends new connection
     */
    @Override
    protected synchronized void act(SocketChannel message) throws Exception {
        AsyncSocketChannel channel=asyncChannelFactory.newAsyncSocketChannel(message);
        ServerConnection connection = new ServerConnection(EchoServer.this, channel);
        connections.put(connection.id, connection);
    }

    /** AsyncServerSocketChannel sends failure
     */
    @Override
    public void postFailure(Throwable exc) {
        exc.printStackTrace();
    }

    //==================== main
    
    public void run(String[] args) throws Exception {
        System.out.println("classPath="+System.getProperty("java.class.path"));
        
        Integer port;
        if (args.length<1) {
//          System.out.println("Usage: EchoServer port maxConn");
//          System.exit(-1);
            port=defaultPort;
        } else {
            port = Integer.valueOf(args[0]);
        }
        int maxConn;
        if (args.length<2) {
            maxConn=1000;
        } else {
            maxConn = Integer.valueOf(args[1]);
        }
        SocketAddress addr=new InetSocketAddress("localhost", port);
        addCloseListener(new CallbackFuture<SocketAddress>()).get(); // inet addr is free now
        System.out.println("EchoServer started");
    }

    public static void main(String[] args) throws Exception {
        System.out.println("classPath="+System.getProperty("java.class.path"));
        
        Integer port;
        if (args.length<1) {
//          System.out.println("Usage: EchoServer port maxConn");
//          System.exit(-1);
            port=defaultPort;
        } else {
            port = Integer.valueOf(args[0]);
        }
        int maxConn;
        if (args.length<2) {
            maxConn=1000;
        } else {
            maxConn = Integer.valueOf(args[1]);
        }
        SocketAddress addr=new InetSocketAddress("localhost", port);
        EchoServer es=new EchoServer(addr, maxConn);
        es.addCloseListener(new CallbackFuture<SocketAddress>()).get(); // inet addr is free now
        System.out.println("EchoServer started");
    }

}
