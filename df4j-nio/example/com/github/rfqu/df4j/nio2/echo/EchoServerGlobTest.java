package com.github.rfqu.df4j.nio2.echo;

import java.net.InetSocketAddress;

import com.github.rfqu.df4j.nio.AsyncChannelFactory;
import com.github.rfqu.df4j.testutil.JavaAppLauncher;

/**
 * requires com.github.rfqu.df4j.ioexample.EchoServer to be launched as an application
 */
public abstract class EchoServerGlobTest extends EchoServerTest {
	
    public EchoServerGlobTest(AsyncChannelFactory asyncChannelFactory) {
        super(asyncChannelFactory);
    }

    public void run(String[] args) throws Exception {
    	String host;
    	if (args.length<1) {
//    		System.out.println("Usage: EchoServerGlobTest host port");
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
    	Process pr=JavaAppLauncher.startJavaApp("com.github.rfqu.df4j.nio2.echo.EchoServer",
    	        Integer.toString(port));
		iaddr = new InetSocketAddress(host, port);
        mediumTest();
        pr.destroy();
    }
}