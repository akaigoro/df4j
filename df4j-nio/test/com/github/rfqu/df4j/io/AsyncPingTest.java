
package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

import com.github.rfqu.df4j.core.SimpleExecutorService;
import com.github.rfqu.df4j.core.Task;
import com.github.rfqu.df4j.core.ThreadFactoryTL;
import com.github.rfqu.df4j.io.AsyncServerSocketChannel;
import com.github.rfqu.df4j.io.AsyncSocketChannel;
import com.github.rfqu.df4j.io.SocketIORequest;
import com.github.rfqu.df4j.util.MessageSink;

/**
 * Requires Java 5.
 */
public class AsyncPingTest {
    private static final Logger log = Logger.getLogger( AsyncPingTest.class ); 
    ExecutorService executor;
    AsyncSelector sel;
    int port = 9998;
    InetSocketAddress local9999;
    PrintStream out=System.out;
    PrintStream err=System.err;
    final int clients=2;//100;
    final int rounds = 3;//1000; // per client
    final int nThreads=Runtime.getRuntime().availableProcessors();

    @Before
    public void init() throws IOException {
        executor = new SimpleExecutorService();
        /*
        ThreadFactoryTL tf=new ThreadFactoryTL();
        executor = Executors.newFixedThreadPool(nThreads, tf);
        tf.setExecutor(executor);
        */        
        out.println("Using " + executor.getClass().getCanonicalName());
        local9999 = new InetSocketAddress("localhost", port);
        log.addAppender(new ConsoleAppender(new SimpleLayout()));
        sel=new AsyncSelector(executor);
        sel.start();
        EchoServer echo=new EchoServer(port);
        echo.start();
    }
    
    @After
    public void shutdown() throws IOException {
        executor.shutdownNow();
        sel.close();
    }

    @Test
    public void testSocketWriteReadThroughput() throws Exception {
        log.setLevel(Level.INFO);
        Task.setCurrentExecutor(executor);
        AsyncSelector.setCurrentSelector(sel);

        MessageSink sink = new MessageSink(clients);
        long start0 = System.nanoTime();
        for (int i = 0; i < clients; i++) {
            new ClientConnection(local9999, rounds, sink);
        }
        
        long start = System.nanoTime();
        float time = (start - start0)/1000000000.0f;
        float rate = clients / time;
        out.printf("%d clients started in %f sec; rate=%f clients/sec \n", clients, time, rate); // actually, round-trips
        sink.await();
        time = (System.nanoTime() - start)/1000000000.0f;
        rate = clients*rounds / time;
        out.printf("Elapsed=%f sec; throughput = %f roundtrips/sec \n", time, rate); // actually, round-trips
    }

    class ClientConnection extends AsyncSocketChannel {
        SocketIORequest req;
        long numOp;
        MessageSink sink;

        public ClientConnection(InetSocketAddress addr, long numOp, MessageSink sink) throws IOException {
            this.numOp = numOp;
            this.sink = sink;
            connect(addr);
            req=new SocketIORequest(4096, false);
            write(req);
        }

        @Override
        public void write(SocketIORequest request) {
            SocketIORequest rq=(SocketIORequest)request;
            if (numOp == 0) {
                sink.send(null);
                log.debug("client closing ...");
                try {
                    super.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return;
            }
            if (numOp < 0) {
                 log.debug("!!! client write started numOp ="+numOp);
                return;
            }
            numOp--;
             log.debug("client write started numOp ="+numOp);
            // switch to write
            ByteBuffer buffer=rq.getBuffer();
            buffer.clear();
            buffer.putLong(numOp); // flip auto
            super.write(rq);
        }

        @Override
        protected void requestCompleted(SocketIORequest request) {
            SocketIORequest rq=(SocketIORequest)request;
            super.requestCompleted(request);
            if (rq.result!=null) {
                if (request.isReadOp()) {
                    ByteBuffer buffer=rq.getBuffer();
                    int rem = buffer.remaining();
                    long n = -1;
                    if (rem >= 8) {
                        n = buffer.getLong();
                    }
                    log.debug("client read ended numOp ="+numOp);
                    write(rq);
                } else {
                    log.debug("client write ended, read started: numOp ="+numOp);
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
        AsyncPingTest t=new AsyncPingTest();
        t.init();
        t.testSocketWriteReadThroughput();
    }
}
