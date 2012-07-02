
package com.github.rfqu.df4j.ioexample;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import com.github.rfqu.df4j.core.*;
import com.github.rfqu.df4j.nio2.*;
import com.github.rfqu.df4j.util.DoubleValue;

public class EchoServerTest {

static class ClientConnection {
    static final int PERIOD = 1; // ms
    static final int BUF_SIZE = 128;
    static AtomicInteger ids=new AtomicInteger(); // DEBUG
    
    public int id=ids.addAndGet(1);
    SerialExecutor serex=new SerialExecutor();
    AsyncSocketChannel channel=new AsyncSocketChannel();
    SocketIORequest request;
    private ByteBuffer buffer;

    AtomicLong rounds;
    Aggregator sink;
    int writeCount=0;

    Random rand=new Random();
    int data;
    long startTime;
    long sum=0;
    long count=0;

    public ClientConnection(InetSocketAddress addr, int rounds, Aggregator sink) throws IOException {
        this.rounds=new AtomicLong(rounds);
        this.sink=sink;
        channel.connect(addr);
        request = new SocketIORequest(BUF_SIZE, false);
        buffer=request.getBuffer();
        startWrite.send(request);
    }

    AsyncHandler<SocketIORequest> startWrite = new AsyncHandler<SocketIORequest>(serex) {
        @Override
        protected void act(SocketIORequest request) throws Exception {
            startTime = System.nanoTime();
            data=rand.nextInt();
            buffer.clear();
            buffer.putInt(data);
            System.out.println("client Request write started id="+id);
            channel.write(request, startRead);
        }
    };

    AsyncHandler<SocketIORequest> startRead = new AsyncHandler<SocketIORequest>(serex) {

        @Override
        protected void act(SocketIORequest message) throws Exception {
            System.out.println("client Request read started id="+id);
            channel.read(request, endRead);
        }
        
    };
    
    AsyncHandler<SocketIORequest> endRead = new AsyncHandler<SocketIORequest>(serex) {
        @Override
        protected void act(SocketIORequest request) throws Exception {
            System.out.println("client Request write ended id="+id);
            // read client's message
            try {
//                    System.out.println("SocketIORequest readCompleted count="+count+"  id="+id+" res="+result);
                int dataFromServer=buffer.getInt();
                if (dataFromServer!=data) {
                    System.out.println("written: "+data+"; read:"+dataFromServer);
                }
                long currentTime = System.currentTimeMillis();
                sum+=(currentTime-startTime);
                count++;
                rounds.decrementAndGet();
                if (rounds.get()==0) {
//                    System.out.println("SocketIORequest finished id="+id);
                    sink.send(new DoubleValue(((double)sum)/count));
                    return;
                }
                long delay=startTime+PERIOD-currentTime;
                if (delay>0) {
                    Timer.schedule(startWrite, request, delay);
                } else {
                    // write it back immediately
//                    startWrite.act(request);
                    startWrite.send(request);
                }
            } catch (Exception e) {
//                System.err.println("SocketIORequest write err: id="+id+":");
                e.printStackTrace();
            }
        }
    };
    
}

/**
 * computes average of input values
 */
static class Aggregator extends Actor<DoubleValue> {
    int numclients;
    long sum=0;
    long counter=0;
    PortFuture<Double> avg=new PortFuture<Double>();

    public Aggregator(int numclients) {
        super(null);
        this.numclients=numclients;
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

    InetSocketAddress local9999 = new InetSocketAddress("localhost", 9998);
    PrintStream out=System.out;
    PrintStream err=System.err;
    int numclients=3000;
    int rounds = 10; // per client
    static int nThreads=Runtime.getRuntime().availableProcessors();

    public void testThroughput(int numclients, int rounds) throws Exception, IOException, InterruptedException {
        this.numclients=numclients;
        this.rounds=rounds;
        Task.setCurrentExecutorService(ThreadFactoryTL.newFixedThreadPool(2));
        EchoServer es=new EchoServer(local9999, numclients);
        
        Task.setCurrentExecutorService(ThreadFactoryTL.newFixedThreadPool(2));
        Aggregator sink = new Aggregator(numclients);

        ClientConnection[] clients=new ClientConnection[numclients];
        long start = System.nanoTime();
        for (int i = 0; i < numclients; i++) {
            clients[i]=new ClientConnection(local9999, rounds, sink);
            Thread.sleep(1);
        }
        out.printf("Started clients:"+numclients);
        double avgLatency=sink.avg.get();
        es.close();
        out.println("all closed");
        float time = (System.nanoTime() - start)/1000000000.0f;
        float rate = numclients*rounds / time;
        out.printf("Elapsed=%f sec; throughput = %f roundtrips/sec \n", time, rate);
        out.printf("Latency=%f msec \n", avgLatency/1000000);
        Timer.shutdown();
    }

    @Test
    public void smokeTest() throws Exception, IOException, InterruptedException {
        testThroughput(1,1);
   }

    @Test
    public void lightTest() throws Exception, IOException, InterruptedException {
        testThroughput(10,3);
   }

    @Test
    public void heavyTest() throws Exception, IOException, InterruptedException {
        testThroughput(1000,10);
   }

    public static void main(String[] args) throws Exception {
        EchoServerTest t=new EchoServerTest();
        t.smokeTest();
    }
}
