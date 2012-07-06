
package com.github.rfqu.df4j.ioexample;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.core.PortFuture;
import com.github.rfqu.df4j.core.Task;
import com.github.rfqu.df4j.core.ThreadFactoryTL;
import com.github.rfqu.df4j.core.Timer;
import com.github.rfqu.df4j.util.DoubleValue;

public class EchoServerTest {
    static final int PERIOD = 5; // ms between subsequent requests for given client
    static final int BUF_SIZE = 128;
    public static final AtomicInteger ids=new AtomicInteger(); // DEBUG
    static int nThreads=Runtime.getRuntime().availableProcessors();

    int availableProcessors = Runtime.getRuntime().availableProcessors();
    InetSocketAddress local9999 = new InetSocketAddress("localhost", 9998);
    PrintStream out=System.out;
    PrintStream err=System.err;
    int numclients=3000;
    int rounds = 10; // per client
	Timer timer;
	EchoServer es;
	ArrayList<ClientConnection> clients=new ArrayList<ClientConnection>();
	
    public void testThroughput(int numclients, int rounds)
    		throws Exception, IOException, InterruptedException
    {
        this.numclients=numclients;
        this.rounds=rounds;
		timer=Timer.getCurrentTimer();
        es=new EchoServer(local9999);
        
        Aggregator sink = new Aggregator(numclients);

        long start = System.currentTimeMillis();
        for (int i = 0; i < numclients; i++) {
            clients.add(new ClientConnection(this, local9999, rounds, sink));
//            System.out.println("clients="+clients.size());
            Thread.sleep(2);
        }
        out.println("Started clients:"+numclients);
        double avgLatency=sink.avg.get();
        es.close();
        out.println("all closed");
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
        long sum=0;
        long counter=0;
        PortFuture<Double> avg=new PortFuture<Double>();
        AtomicInteger ac=new AtomicInteger();

        public Aggregator(int numclients) {
            super(null);
            this.numclients=numclients;
        }

        @Override
        public void send(DoubleValue m) {
            ac.addAndGet(1);
            super.send(m);
        }

        @Override
        protected void act(DoubleValue message) throws Exception {
            counter++;
            sum+=message.value;
            if (counter==numclients) {
                complete();
            }
        }
        
        @Override
        protected void complete() throws Exception {
            avg.send(((double)sum)/counter);
        }

    }

    @Test
    public void smokeTest() throws Exception, IOException, InterruptedException {
        testThroughput(1,1);
   }

    @Test
    public void lightTest() throws Exception, IOException, InterruptedException {
        testThroughput(5,5);
   }

    @Test
    public void mediumTest() throws Exception, IOException, InterruptedException {
        testThroughput(100,200);
   }

//    @Test
    public void heavyTest() throws Exception, IOException, InterruptedException {
        testThroughput(1000,200);
   }

 //   @Test
    public void veryHeavyTest() throws Exception, IOException, InterruptedException {
        testThroughput(10000,1000);
   }

    public static void main(String[] args) throws Exception {
        EchoServerTest t=new EchoServerTest();
        t.smokeTest();
        t.lightTest();
        t.heavyTest();
    }
}
