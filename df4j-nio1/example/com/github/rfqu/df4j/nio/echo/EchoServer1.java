package com.github.rfqu.df4j.nio.echo;

import java.io.IOException;
import java.net.SocketAddress;

public class EchoServer1 extends EchoServer {

    public EchoServer1(SocketAddress addr, int maxConn) throws IOException {
        super(addr, maxConn);
        // TODO Auto-generated constructor stub
    }

    /** Runs {@EchoServer} with nio-1.
    * 
    * To run tests, {@see EchoServerLockTest1} and {@see EchoServerGlobTest1}.
    */
    public static void main(String[] args) throws Exception {
        EchoServer.main(args);
    }

}