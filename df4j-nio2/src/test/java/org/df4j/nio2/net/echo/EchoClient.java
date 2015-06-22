package org.df4j.nio2.net.echo;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.df4j.nio2.df4j.core.Actor;
import org.df4j.nio2.df4j.core.Callback;
import org.df4j.nio2.df4j.core.CompletableFuture;
import org.df4j.nio2.df4j.core.Timer;
import org.df4j.nio2.df4j.testutil.DoubleValue;

/**
 * requires EchoServer to be launched.
 */
public class EchoClient {
	static final long PERIOD = 0;//5; // ms between subsequent requests for given client
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
	
    public void clientFinished(ClientConnection clientConnection, DoubleValue avg) {
		sink.post(avg);
		clients.remove(clientConnection.id);
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
            ClientConnection cconn = new ClientConnection(iaddr, rounds);
			clients.put(cconn.id, cconn);
            cconn.addListener((Callback)sink);
			cconn.start();
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
        CompletableFuture<Double> avg=new CompletableFuture<Double>();
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
            	out.printf("finished clients=%d; clients/sec=%f \n", counter, rate);
            	startTime=curTime;
    		}
            if (counter==numclients) {
                complete();
            }
        }
        
        @Override
        protected void complete() throws Exception {
            avg.post(sum/counter);
        }

    }

}