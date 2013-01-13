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

public class EchoServer1 extends EchoServer {
        
    public EchoServer1(AsyncChannelFactory asyncChannelFactory, SocketAddress addr, int maxConn) throws IOException {
        this.asyncChannelFactory=asyncChannelFactory;
        this.addr=addr;
        assch=asyncChannelFactory.newAsyncServerSocketChannel(addr, this);
        assch.up(maxConn);
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
        EchoServer1 es=new EchoServer1(addr, maxConn);
        es.addCloseListener(new CallbackFuture<SocketAddress>()).get(); // inet addr is free now
        System.out.println("EchoServer started");
    }

}
