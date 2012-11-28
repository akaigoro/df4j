package com.github.rfqu.df4j.nio2.echo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.rfqu.df4j.core.ActorVariable;
import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.CallbackFuture;
import com.github.rfqu.df4j.core.DataflowVariable;
import com.github.rfqu.df4j.nio2.AsyncServerSocketChannel;
import com.github.rfqu.df4j.nio2.AsyncSocketChannel;

public class EchoServer extends DataflowVariable {
	public static final int defaultPort = 9993;
    public static final int numconnections=100; // max simultaneous server connections
    public static final int BUF_SIZE = 128;

	Semafor maxConn=new Semafor();
	Semafor pending=new Semafor();
	
	AtomicInteger ids=new AtomicInteger(); // for DEBUG    
    SocketAddress addr;
    AsyncServerSocketChannel assch;
	AcceptHandler acceptHandler=new AcceptHandler();
    /** active connections */
    HashMap<Integer, ServerConnection> connections=new HashMap<Integer, ServerConnection>();
    /** listeners to the closing event */
        
    public EchoServer(SocketAddress addr, int maxConn) throws IOException {
        this.addr=addr;
        assch=new AsyncServerSocketChannel(addr);
        this.maxConn.up(maxConn);
        pending.up();
    }

    public <R extends Callback<SocketAddress>> R addCloseListener(R listener) {
    	return assch.addCloseListener(listener);
    }

    protected void connClosed(ServerConnection connection) {
        synchronized(this) {
            connections.remove(connection.id);
            if (assch.isClosed()) {
                return;
            }
        }
        //            System.out.println("connections="+connections.size());
        maxConn.up(); // allow next accept
    }

//    @Override
    public void close() {
        if (assch.isClosed()) {
            return;
        }
        assch.close();
        for (ServerConnection connection: connections.values()) {
        	connection.close();
        }
    }

    //======================= backend
    
	@Override
	protected void act() {
        assch.accept(acceptHandler);
	}
	
    //==================== inner classes
	
    class AcceptHandler extends ActorVariable<AsynchronousSocketChannel> {
        /** AsyncServerSocketChannel sends new connection
         */
    	@Override
    	protected void act(AsynchronousSocketChannel message) throws Exception {
        	AsyncSocketChannel channel=new AsyncSocketChannel(message);
            ServerConnection connection = new ServerConnection(EchoServer.this, channel);
            connections.put(connection.id, connection);
            pending.up();
        }

        /** AsyncServerSocketChannel sends failure
         */
        @Override
        public void sendFailure(Throwable exc) {
        	exc.printStackTrace();
        }
        
    }
    //==================== main
    
    public static void main(String[] args) throws Exception {
    	System.out.println("classPath="+System.getProperty("java.class.path"));
    	
    	Integer port;
    	if (args.length<1) {
//    		System.out.println("Usage: EchoServer port maxConn");
//    		System.exit(-1);
    		port=defaultPort;
    	} else {
    	    port = Integer.valueOf(args[0]);
    	}
    	int maxConn;
    	if (args.length<2) {
    		maxConn=numconnections;
    	} else {
    		maxConn = Integer.valueOf(args[1]);
    	}
		SocketAddress addr=new InetSocketAddress("localhost", port);
        EchoServer es=new EchoServer(addr, maxConn);
        es.addCloseListener(new CallbackFuture<SocketAddress>()).get(); // inet addr is free now
        System.out.println("EchoServer started");
    }

}