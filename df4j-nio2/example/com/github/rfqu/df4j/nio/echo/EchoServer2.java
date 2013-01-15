package com.github.rfqu.df4j.nio.echo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.rfqu.df4j.core.ActorVariable;
import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.CallbackFuture;
import com.github.rfqu.df4j.nio.AsyncChannelFactory;
import com.github.rfqu.df4j.nio.AsyncChannelFactory2;
import com.github.rfqu.df4j.nio.AsyncServerSocketChannel2;
import com.github.rfqu.df4j.nio.AsyncSocketChannel;
import com.github.rfqu.df4j.nio.echo.EchoServer;

public class EchoServer2 extends EchoServer  {
        
    public EchoServer2(AsyncChannelFactory asyncChannelFactory, SocketAddress addr, int maxConn) throws IOException {
        super(asyncChannelFactory, addr, maxConn);
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
        EchoServer2 es=new EchoServer2(new AsyncChannelFactory2(), addr, maxConn);
        es.addCloseListener(new CallbackFuture<SocketAddress>()).get(); // inet addr is free now
        System.out.println("EchoServer started");
    }

}