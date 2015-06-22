package org.df4j.nio2.net.echo;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;

import org.junit.Test;

/**
 * launches EchoServer as an application
 */
public class EchoServerGlobTest {//extends EchoServerTest {
    static PrintStream out=System.out;
    static PrintStream err=System.err;

    EchoClient t=new EchoClient();

    public void globTest(int maxConn, int numclients, int rounds) throws Exception  {
        t.testThroughput(numclients, rounds);
    }

    @Test
    public void smokeTest() throws Exception, IOException, InterruptedException {
        t.testThroughput(1,1);
   }
    @Test
    public void smokeTest1() throws Exception, IOException, InterruptedException {
        t.testThroughput(1,2);
   }
//    @Test
    public void smokeTest2() throws Exception, IOException, InterruptedException {
        t.testThroughput(2,1);
   }

    @Test
    public void lightTest() throws Exception, IOException, InterruptedException {
        t.testThroughput(20,200);
   }

    @Test
    public void mediumTest() throws Exception, IOException, InterruptedException {
        t.testThroughput(100,100);
   }

    @Test
    public void heavyTest() throws Exception, IOException, InterruptedException {
        t.testThroughput(1000,10);
   }

//    @Test
    public void veryHeavyTest() throws Exception, IOException, InterruptedException {
        t.testThroughput(5000,20);
   }	
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
        Thread.sleep(500); // let echo server start
        EchoServerGlobTest t=new EchoServerGlobTest();
        t.t.iaddr = new InetSocketAddress(host, port);
        t.mediumTest();
        t.heavyTest();
        t.veryHeavyTest();
        pr.destroy();
    }
}