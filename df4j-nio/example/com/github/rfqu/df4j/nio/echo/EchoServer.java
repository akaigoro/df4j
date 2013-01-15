package com.github.rfqu.df4j.nio.echo;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.rfqu.df4j.core.ActorVariable;
import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.nio.AsyncChannelFactory;
import com.github.rfqu.df4j.nio.AsyncServerSocketChannel;
import com.github.rfqu.df4j.nio.AsyncSocketChannel;

public class EchoServer extends ActorVariable<AsyncSocketChannel>
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
    protected synchronized void act(AsyncSocketChannel channel) throws Exception {
        ServerConnection connection = new ServerConnection(EchoServer.this, channel);
        connections.put(connection.id, connection);
    }

    /** AsyncServerSocketChannel sends failure
     */
    @Override
    public void postFailure(Throwable exc) {
        exc.printStackTrace();
    }

 }
