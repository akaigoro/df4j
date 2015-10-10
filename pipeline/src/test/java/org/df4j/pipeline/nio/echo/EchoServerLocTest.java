package org.df4j.pipeline.nio.echo;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import org.df4j.pipeline.df4j.core.DFContext;
import org.junit.BeforeClass;
import org.junit.Test;

public class EchoServerLocTest {
    static PrintStream out=System.out;
    static PrintStream err=System.err;

    EchoClient t=new EchoClient();

    @BeforeClass
    public static void initClass() {
        DFContext.setSingleThreadExecutor();
    }

    public void localTest(int maxConn, int numclients, int rounds) throws Exception  {
        EchoServer es = new EchoServer(t.iaddr, maxConn);
        es.start();
        Thread.sleep(100);
        try {
            t.testThroughput(numclients, rounds);
        } finally {
            es.close(); // start closing process
            es.getFuture().get();
        }
        out.println("all closed");
    }

    @Test
    public void smokeTest() throws Exception, IOException, InterruptedException {
    	localTest(1, 1,1);
    }

    @Test
    public void lightTest() throws Exception, IOException, InterruptedException {
    	localTest(1, 2,1);
     }
 
    @Test
    public void mediumTest() throws Exception, IOException, InterruptedException {
    	localTest(30, 100,200);
   }

//    @Test
    public void heavyTest() throws Exception, IOException, InterruptedException {
    	localTest(200, 1000, 10);
   }

//    @Test
    public void veryHeavyTest() throws Exception, IOException, InterruptedException {
    	localTest(2000, 2500, 100);
   }

    public static void main(String[] args) throws IOException, InterruptedException, Exception {
        String host;
        if (args.length<1) {
//          System.out.println("Usage: EchoServerTest host port");
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
        EchoServerLocTest t=new EchoServerLocTest();
        t.t.iaddr = new InetSocketAddress(host, port);
        t.smokeTest();
    }
}
