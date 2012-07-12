package com.github.rfqu.df4j.ioexample;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.core.PortFuture;
import com.github.rfqu.df4j.core.Timer;
import com.github.rfqu.df4j.util.DoubleValue;

/**
 * requires com.github.rfqu.df4j.ioexample.EchoServer to be launched
 */
public class EchoServerGlobTest {
	static final long PERIOD = 5; // ms between subsequent requests for given client
    static final int BUF_SIZE = 128;
    public static final AtomicInteger ids=new AtomicInteger(); // DEBUG
    static int nThreads=Runtime.getRuntime().availableProcessors();
    static int availableProcessors = Runtime.getRuntime().availableProcessors();
    static PrintStream out=System.out;
    static PrintStream err=System.err;

	InetSocketAddress iaddr = new InetSocketAddress("localhost", EchoServer.defaultPort);
	int numclients;
    int rounds; // per client
	Timer timer;
	HashMap<Integer, ClientConnection> clients=new HashMap<Integer, ClientConnection>();
    Aggregator sink;
	
    public synchronized void removeClientConnection(ClientConnection clientConnection) {
        clients.remove(clientConnection.id);
    }

	public void clientFinished(ClientConnection clientConnection, DoubleValue avg) {
		sink.send(avg);
		removeClientConnection(clientConnection);
	}

    public void testThroughput(int numclients, int rounds)
    		throws Exception, IOException, InterruptedException
    {
        this.numclients=numclients;
        this.rounds=rounds;
		timer=Timer.getCurrentTimer();
        sink = new Aggregator(numclients);
        
        long start = System.currentTimeMillis();
        for (int i = 0; i < numclients; i++) {
            ClientConnection cconn = new ClientConnection(this, iaddr, rounds);
			clients.put(cconn.id, cconn);
			PortFuture<AsynchronousSocketChannel> listener=new PortFuture<AsynchronousSocketChannel>();
			cconn.addConnectListener(listener);
			listener.get(); // wait connection to connect
//            Thread.sleep(2);
        }
        out.println("Started clients:"+numclients);
        double avgLatency=sink.avg.get();
        float time = (System.currentTimeMillis() - start)/1000.0f; // sec
        float rate = numclients*rounds / time;
        out.printf("Elapsed=%f sec; throughput = %f roundtrips/sec \n", time, rate);
        out.printf("Latency=%f msec \n", avgLatency);
        timer.shutdown().get();
        out.println("============================== test finished");
    }

    /**
     * computes average of input values
     */
    static class Aggregator extends Actor<DoubleValue> {
        int numclients;
		int reportPeriod = 1000;
        double sum=0;
        long counter=0;
        PortFuture<Double> avg=new PortFuture<Double>();
        long startTime=System.currentTimeMillis();

        public Aggregator(int numclients) {
            this.numclients=numclients;
        }

        @Override
        protected void act(DoubleValue message) throws Exception {
            counter++;
            sum+=message.value;
			if (counter%reportPeriod==0) {
    			long curTime=System.currentTimeMillis();
            	double rate=(reportPeriod*1000d)/(curTime-startTime);
            	out.printf("finished clients=%d; clients/sec=%f roundtrips/sec \n", counter, rate);
            	startTime=curTime;
    		}
            if (counter==numclients) {
                complete();
            }
        }
        
        @Override
        protected void complete() throws Exception {
            avg.send(sum/counter);
        }

    }

    @Test
    public void smokeTest() throws Exception, IOException, InterruptedException {
    	testThroughput(1,1);
   }

    @Test
    public void lightTest() throws Exception, IOException, InterruptedException {
    	testThroughput(2,2);
   }

    @Test
    public void mediumTest() throws Exception, IOException, InterruptedException {
    	testThroughput(100,200);
   }

    @Test
    public void heavyTest() throws Exception, IOException, InterruptedException {
    	testThroughput(1000,100);
   }

//    @Test
    public void veryHeavyTest() throws Exception, IOException, InterruptedException {
    	testThroughput(10000,1000);
   }

    public static void main(String[] args) throws Exception {
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
    	EchoServerGlobTest t=new EchoServerGlobTest();
		t.iaddr = new InetSocketAddress(host, port);
        t.mediumTest();
    }

}
