package com.github.rfqu.df4j.nio.echo;

import java.net.InetSocketAddress;

/**
 * requires com.github.rfqu.df4j.ioexample.EchoServer to be launched as an application
 */
public class EchoServerGlobTest extends EchoServerTest {
	
    public static void main(String[] args) throws Exception {
        String host;
        if (args.length<1) {
//          System.out.println("Usage: EchoServerGlobTest host port");
//          System.exit(-1);
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
        final String name = EchoServer.class.getName();
        Process pr=JavaAppLauncher.startJavaApp(name,
                Integer.toString(port));
        EchoServerGlobTest t=new EchoServerGlobTest();
        t.iaddr = new InetSocketAddress(host, port);
        t.mediumTest();
        pr.destroy();
    }
}