package com.github.rfqu.df4j.nio2.echo2;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;

import org.junit.Test;

import com.github.rfqu.df4j.core.CallbackFuture;

public class EchoServerLocTest {
    static PrintStream out=System.out;
    static PrintStream err=System.err;

    EchoServerGlobTest t=new EchoServerGlobTest();
	EchoServer es;
	
    public void localTest(int maxConn, int numclients, int rounds) throws Exception  {
        es=new EchoServer(t.iaddr, maxConn);
        Thread.sleep(100);
        t.testThroughput(numclients, rounds);
        es.close(); // start closing process
        es.addCloseListener(new CallbackFuture<InetSocketAddress>()).get(); // inet addr is free now
        out.println("all closed");
    }

    @Test
    public void smokeTest() throws Exception, IOException, InterruptedException {
    	localTest(1, 1,1);
   }

    @Test
    public void lightTest() throws Exception, IOException, InterruptedException {
    	localTest(2, 2,2);
   }

    @Test
    public void mediumTest() throws Exception, IOException, InterruptedException {
    	localTest(100, 100,200);
   }

    @Test
    public void heavyTest() throws Exception, IOException, InterruptedException {
    	localTest(500, 1000, 10);
   }

    @Test
    public void veryHeavyTest() throws Exception, IOException, InterruptedException {
    	localTest(3000, 5000, 10);
   }

    public static void main(String[] args) throws Exception {
    	String host;
    	if (args.length<1) {
//    		System.out.println("Usage: EchoServerTest host port");
//    		System.exit(-1);
    		host="localhost";
    	} else {
    		host = args[0];
    	}
    	Integer port;
    	if (args.length<2) {
    		port=EchoServer.defaultPort;
    	} else {
    	    port = Integer.valueOf(args[1]);
    	}
    	EchoServerLocTest t=new EchoServerLocTest();
		t.t.iaddr = new InetSocketAddress(host, port);
        t.veryHeavyTest();
    }

}
