package com.github.rfqu.df4j.nio.echo;

import java.io.IOException;
import java.net.SocketAddress;

public class EchoServer2 extends EchoServer {

    public EchoServer2(SocketAddress addr, int maxConn) throws IOException {
        super(addr, maxConn);
        // TODO Auto-generated constructor stub
    }

    /** Runs {@EchoServer} with nio-2.
    * 
    * To run tests, {@see EchoServerLockTest2} and {@see EchoServerGlobTest2}.
    */
    public static void main(String[] args) throws Exception {
        EchoServer.main(args);
    }

}