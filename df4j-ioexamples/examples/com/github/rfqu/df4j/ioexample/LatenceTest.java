
package com.github.rfqu.df4j.ioexample;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

import com.github.rfqu.df4j.core.Task;
import com.github.rfqu.df4j.core.ThreadFactoryTL;
import com.github.rfqu.df4j.io.AsyncServerSocketChannel;
import com.github.rfqu.df4j.io.AsyncSocketChannel;
import com.github.rfqu.df4j.io.SocketIORequest;
import com.github.rfqu.df4j.util.MessageSink;

public class LatenceTest {
    private static final Logger log = Logger.getLogger( LatenceTest.class ); 
    ExecutorService executor;
    InetSocketAddress local9999;
    PrintStream out=System.out;
    PrintStream err=System.err;
    int clients=10;
    int rounds = 10000; // per client
    private int nThreads=Runtime.getRuntime().availableProcessors();

    @Before
    public void init() {
//        executor = new SimpleExecutorService();
        ThreadFactoryTL tf=new ThreadFactoryTL();
       executor = Executors.newFixedThreadPool(nThreads, tf);
       tf.setExecutor(executor);        
        out.println("Using " + executor.getClass().getCanonicalName());
        local9999 = new InetSocketAddress("localhost", 9998);
        log.addAppender(new ConsoleAppender(new SimpleLayout()));
    }
    
    @Test
    public void testServerSocket() throws Exception {
        log.setLevel(Level.ALL);
        Task.setCurrentExecutor(executor);
        AsyncServerSocketChannel s=new AsyncServerSocketChannel(local9999);
        ServerConnection sc = new ServerConnection(s);

        MessageSink sink = new MessageSink(1);
        ClientConnection cc = new ClientConnection(local9999, 1, sink);
        sink.await();
        s.close();
    }

    @Test
    public void testSocketWriteReadThroughput() throws Exception {
        log.setLevel(Level.INFO);
        Task.setCurrentExecutor(executor);
        AsyncServerSocketChannel s=new AsyncServerSocketChannel(local9999);
        ServerConnection sc = new ServerConnection(s);

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
        s.close();
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
        out.printf("mean time="+mean/1000+" mks");
    }

    class CountingReqiest extends SocketIORequest {
        int readcount;
        int writecount;
        long starttime;
        
        public CountingReqiest() {
            super(4096, true);
        }
    }

    class ServerConnection extends AsyncSocketChannel {
        AsyncServerSocketChannel s;
        CountingReqiest request=new CountingReqiest();
        
        public ServerConnection(AsyncServerSocketChannel s) {
            this.s=s;
            super.connect(s);
            read(request);
            log.debug("    server read started");
        }

        @Override
        protected void connCompleted() {
            super.connCompleted();
            new ServerConnection(s);
        }

        @Override
        protected void requestCompleted(SocketIORequest request) {
            CountingReqiest rq=(CountingReqiest)request;
            super.requestCompleted(request);
            if (rq.result!=null) {
                if (request.isReadOp()) {
                    if (rq.result == -1) {
                        log.debug("    server closing");
                        return; // client has closed connection
                    }
                    rq.readcount++;
                    ByteBuffer buffer=rq.getBuffer();
                    int rem = buffer.remaining();
                    long n = buffer.getLong();
//                    log.debug("    server read ended count="+rq.readcount+" rem="+rem+" n="+n+" res="+rq.result);
                    // switch to write
                    buffer.clear();
                    buffer.putLong(n); // flip auto
                    super.write(rq);
                } else {
                    rq.writecount++;
//                    log.debug("    server write ended count="+rq.writecount);
                    super.read(rq);
                }
            } else {
                // TODO gracefully shutdown when client closed connection
                try {
                    super.close();
                    return;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
//                    log.debug("    ServerIOR.channel.close failed:");
                    e.printStackTrace();
                }
                if (!channel.isOpen()) {
                    log.debug("    server channel closed");
                    return;
                }
 //               log.debug("    ServerIOR "+(rq.isReadOp()?"read":"write")+" Failed:");
                rq.exc.printStackTrace();
            }
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
        LatenceTest t=new LatenceTest();
        t.init();
        t.testServerSocket();
    }
}
