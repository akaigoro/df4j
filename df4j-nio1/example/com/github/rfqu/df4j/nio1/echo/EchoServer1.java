package com.github.rfqu.df4j.nio1.echo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import com.github.rfqu.df4j.core.CallbackFuture;
import com.github.rfqu.df4j.nio.AsyncChannelFactory;
import com.github.rfqu.df4j.nio1.AsyncChannelFactory1;
import com.github.rfqu.df4j.nio2.echo.EchoServer;

public class EchoServer1 extends EchoServer {
        
    public EchoServer1(AsyncChannelFactory asyncChannelFactory, SocketAddress addr, int maxConn) throws IOException {
        super(asyncChannelFactory, addr, maxConn);
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
        EchoServer1 es=new EchoServer1(new AsyncChannelFactory1(), addr, maxConn);
        es.addCloseListener(new CallbackFuture<SocketAddress>()).get(); // inet addr is free now
        System.out.println("EchoServer started");
    }

}
