package com.github.rfqu.df4j.ioexample;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.rfqu.df4j.core.SimpleExecutorService;
import com.github.rfqu.df4j.core.Task;
import com.github.rfqu.df4j.io.AsyncSocketChannel;
import com.github.rfqu.df4j.io.SocketIORequest;
import com.github.rfqu.df4j.util.MessageSink;

public class LatencyTest {
    static final Logger log = Logger.getLogger( LatencyTest.class ); 
    PrintStream out=System.out;
    PrintStream err=System.err;
    int port = 9998;
    InetSocketAddress local9999;
    ExecutorService executor;
    EchoServer echoServer;
    int clients=100;
    int rounds = 1000; // per client

    @Before
    public void init() throws IOException {
        log.addAppender(new ConsoleAppender(new SimpleLayout()));
        int port = 9998;
        local9999 = new InetSocketAddress("localhost", port);
        executor = new SimpleExecutorService();
        /*
        int nThreads=Runtime.getRuntime().availableProcessors();
        ThreadFactoryTL tf=new ThreadFactoryTL();
        executor = Executors.newFixedThreadPool(nThreads, tf);
        tf.setExecutor(executor);
        */        
        out.println("Using " + executor.getClass().getCanonicalName());
        echoServer=new EchoServer(port);
        echoServer.start();
    }
    
    @After
    public void shutdown() throws IOException {
        echoServer.close();
        executor.shutdown();
    }
    
//    @Test
    public void testServerSocket() throws Exception {
        log.setLevel(Level.ALL);
        Task.setCurrentExecutor(executor);

        MessageSink sink = new MessageSink(1);
        ClientConnection cc = new ClientConnection(local9999, 1, sink);
        sink.await();
    }

    @Test
    public void testSocketWriteReadThroughput() throws Exception {
        log.setLevel(Level.INFO);
        Task.setCurrentExecutor(executor);

        MessageSink sink = new MessageSink(clients);
        long start0 = System.nanoTime();
        ClientConnection[] ccs=new ClientConnection[clients];
        for (int i = 0; i < clients; i++) {
            ccs[i]=new ClientConnection(local9999, rounds, sink);
        }
        
        long start = System.nanoTime();
        float time = (start - start0)/1000000000.0f;
        float rate = clients / time;
        out.printf("%d clients started in %f sec; rate=%f clients/sec \n", clients, time, rate); // actually, round-trips
        sink.await();
        time = (System.nanoTime() - start)/1000000000.0f;
        rate = clients*rounds / time;
        out.printf("Elapsed=%f sec; throughput = %f roundtrips/sec \n", time, rate); // actually, round-trips
        long sum=0;
        int count=0;
        for (int i = 0; i < clients; i++) {
            count+=ccs[i].count;
            sum+=ccs[i].sum;
        }
        float mean=((float)sum)/count;
        out.printf("mean time=%f mks \n", mean/1000);
    }

    class CountingReqiest extends SocketIORequest {
        int readcount;
        int writecount;
        long starttime;
        
        public CountingReqiest() {
            super(4096, true);
        }
    }

    class ClientConnection extends AsyncSocketChannel {
        CountingReqiest req;
        long numOp;
        MessageSink sink;
        int count=0;
        long sum=0;

        public ClientConnection(InetSocketAddress addr, long numOp, MessageSink sink) throws IOException {
            this.numOp = numOp;
            this.sink = sink;
            connect(addr);
            req=new CountingReqiest();
            write(req);
        }

        @Override
        public void write(SocketIORequest request) {
            CountingReqiest rq=(CountingReqiest)request;
            if (numOp == 0) {
                sink.send(null);
                 log.debug("client close; readcount="+rq.readcount+" writecount="+rq.writecount);
                try {
                    super.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return;
            }
            if (numOp < 0) {
                 log.debug("!!! client write started numOp ="+numOp+"!!! writecount="+rq.writecount);
                return;
            }
            rq.starttime=System.nanoTime();
            numOp--;
             log.debug("client write started numOp ="+numOp+" writecount="+rq.writecount);
            // switch to write
            ByteBuffer buffer=rq.getBuffer();
            buffer.clear();
            buffer.putLong(numOp); // flip auto
            super.write(rq);
        }

        @Override
        protected void requestCompleted(SocketIORequest request) {
            CountingReqiest rq=(CountingReqiest)request;
            super.requestCompleted(request);
            if (rq.result!=null) {
                if (request.isReadOp()) {
                    rq.readcount++;
                    ByteBuffer buffer=rq.getBuffer();
                    int rem = buffer.remaining();
                    long n = -1;
                    if (rem >= 8) {
                        n = buffer.getLong();
                    }
                    log.debug("client read ended numOp ="+numOp+" readcount="+rq.readcount+" rem="+rem+" n="+n);
                    write(rq);
                } else {
                    rq.writecount++;
                    log.debug("client write ended, read started: numOp ="+numOp+" writecount="+rq.writecount);
                    long lat=System.nanoTime()-rq.starttime;
//                    log.info("lat="+lat);
                    count++;
                    sum+=lat;
                    super.read(rq);
                }
            } else {
                // TODO Auto-generated method stub
                log.debug("    ClientIOR "+(rq.isReadOp()?"read":"write")+" Failed:");
                rq.exc.printStackTrace();
            }
        }

    }

    public static void main(String[] args) throws Exception {
        LatencyTest t=new LatencyTest();
        t.init();
        t.testServerSocket();
    }
}
