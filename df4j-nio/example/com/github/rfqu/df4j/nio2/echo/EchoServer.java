package com.github.rfqu.df4j.nio2.echo;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.CallbackFuture;
import com.github.rfqu.df4j.core.Promise;
import com.github.rfqu.df4j.nio2.AsyncServerSocketChannel;
import com.github.rfqu.df4j.nio2.AsyncSocketChannel;

public class EchoServer
    implements Callback<AsyncSocketChannel>,
    Closeable
{
	public static final int defaultPort = 9993;
    public static final int numconnections=100; // max simultaneous server connections
    public static final int BUF_SIZE = 128;

    AtomicInteger ids=new AtomicInteger(); // for DEBUG    
    InetSocketAddress addr;
    AsyncServerSocketChannel assch;
    /** active connections */
    HashMap<Integer, ServerConnection> connections=new HashMap<Integer, ServerConnection>();
    /** listeners to the closing event */
    protected Promise<InetSocketAddress> closeEvent=new Promise<InetSocketAddress>();
        
    public EchoServer(InetSocketAddress addr, int maxConn) throws IOException {
        this.addr=addr;
        assch=new AsyncServerSocketChannel(addr, this, maxConn);
    }

    public <R extends Callback<InetSocketAddress>> R addCloseListener(R listener) {
        closeEvent.addListener(listener);
        return listener;
    }

    protected void connClosed(ServerConnection connection) {
        synchronized(this) {
            connections.remove(connection.id);
            if (!assch.isOpened()) {
                return;
            }
        }
        //            System.out.println("connections="+connections.size());
        assch.upConnNumber(); // allow accept
    }

    @Override
    public void close() {
        synchronized (this) {
            if (!assch.isOpened()) {
                return;
            }
        }
        try {
            assch.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        for (;;) {
            synchronized (this) {
                Set<Integer> keys = connections.keySet();
                Iterator<Integer> it = keys.iterator();
                if (!it.hasNext()) {
                    break;
                }
                Integer firstKey = it.next();
                connections.get(firstKey).close(); // removes from the collection
                connections.remove(firstKey);
            }
        }
        closeEvent.send(addr);
    }

    //======================= backend
    
    /** AsyncServerSocketChannel sends new connection
     */
    @Override
    public void send(AsyncSocketChannel channel) {
        ServerConnection connection = new ServerConnection(this, channel);
        synchronized(this) {
            connections.put(connection.id, connection);
        }
    }

    /** AsyncServerSocketChannel sends failure
     */
    @Override
    public void sendFailure(Throwable exc) {
    }
    
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
		InetSocketAddress addr=new InetSocketAddress("localhost", port);
        EchoServer es=new EchoServer(addr, maxConn);
        es.addCloseListener(new CallbackFuture<InetSocketAddress>()).get(); // inet addr is free now
        System.out.println("EchoServer started");
    }

}