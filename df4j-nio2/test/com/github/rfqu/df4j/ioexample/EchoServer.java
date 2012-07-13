package com.github.rfqu.df4j.ioexample;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.rfqu.df4j.core.Port;
import com.github.rfqu.df4j.core.PortFuture;
import com.github.rfqu.df4j.core.Promise;
import com.github.rfqu.df4j.core.StreamPort;
import com.github.rfqu.df4j.nio2.AsyncServerSocketChannel;
import com.github.rfqu.df4j.nio2.AsyncSocketChannel;

public class EchoServer extends AsyncServerSocketChannel
  implements StreamPort<AsynchronousSocketChannel>
{
	public static final int defaultPort = 9998;
    public static final int numconnections=100; // max simultaneous server connections
    public static final int BUF_SIZE = 128;

    AtomicInteger ids=new AtomicInteger(); // for DEBUG    
    InetSocketAddress addr;
    /** active connections */
    HashMap<Integer, ServerConnection> connections=new HashMap<Integer, ServerConnection>();
    /** listeners to the closing event */
    protected Promise<InetSocketAddress> closeListeners=new Promise<InetSocketAddress>();
        
    public EchoServer(InetSocketAddress addr, int maxConn) throws IOException {
        super(addr);
        super.open(this, maxConn);
        this.addr=addr;
    }

    @Override
    public void send(AsynchronousSocketChannel m) {
        AsyncSocketChannel channel=new AsyncSocketChannel(m);
        try {
            ServerConnection connection = new ServerConnection(this, channel);
            synchronized(this) {
                connections.put(connection.id, connection);
            }
        } catch (ClosedChannelException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        synchronized (this) {
            if (!opened) {
                return;
            }
        }
        super.close();
        for (;;) {
            synchronized (this) {
                Set<Integer> keys = connections.keySet();
                Iterator<Integer> it = keys.iterator();
                if (!it.hasNext()) {
                    break;
                }
                try {
                    Integer firstKey = it.next();
                    connections.get(firstKey).close(); // removes from the collection
                    connections.remove(firstKey);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        closeListeners.send(addr);
    }

    protected void conncClosed(ServerConnection connection) {
        synchronized(this) {
            connections.remove(connection.id);
            if (!opened) {
                return;
            }
        }
        //            System.out.println("connections="+connections.size());
        super.maxConnUp(); // allow another connection
    }

    public <R extends Port<InetSocketAddress>> R addCloseListener(R listener) {
        closeListeners.addListener(listener);
        return listener;
    }

    public static void main(String[] args) throws Exception {
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
        es.addCloseListener(new PortFuture<InetSocketAddress>()).get(); // inet addr is free now
    }
    
}