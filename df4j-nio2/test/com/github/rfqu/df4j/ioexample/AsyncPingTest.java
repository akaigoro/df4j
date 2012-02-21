
package com.github.rfqu.df4j.ioexample;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import com.github.rfqu.df4j.core.SimpleExecutorService;
import com.github.rfqu.df4j.core.Task;
import com.github.rfqu.df4j.core.ThreadFactoryTL;
import com.github.rfqu.df4j.io.AsyncServerSocketChannel;
import com.github.rfqu.df4j.io.AsyncSocketChannel;
import com.github.rfqu.df4j.io.SocketIORequest;
import com.github.rfqu.df4j.util.MessageSink;

/**
 * Requires Java 7.
 */
public class AsyncPingTest {
    PrintStream out=System.out;
    PrintStream err=System.err;
    int clients=2000;//300;
    int rounds = 100; // per client
    int nThreads=Runtime.getRuntime().availableProcessors();
    InetSocketAddress local9999 = new InetSocketAddress("localhost", 9998);
    AtomicLong cStartedR=new AtomicLong();
    AtomicLong cEndedR=new AtomicLong();
    AtomicLong cStartedW=new AtomicLong();
    AtomicLong cEndedW=new AtomicLong();
    AtomicLong sStartedR=new AtomicLong();
    AtomicLong sEndedR=new AtomicLong();
    AtomicLong sStartedW=new AtomicLong();
    AtomicLong sEndedW=new AtomicLong();

    class ServerConnection extends AsyncSocketChannel {
        AsynchronousServerSocketChannel assch;
        ServerIOR request;
        
        public ServerConnection(AsynchronousServerSocketChannel assch) {
            this.assch=assch;
            super.accept(assch);
        }

        @Override
        protected void connCompleted() {
            super.connCompleted();
            new ServerConnection(assch);
            request=new ServerIOR();
            request.read(this);
        }

        class ServerIOR extends SocketIORequest {
            int readcount;
            int writecount;

            public ServerIOR() {
                super(4096, true);
                this.channel = ServerConnection.this;
            }

            @Override
            public void readCompleted(Integer result) {
                sEndedR.incrementAndGet();
                readcount++;
                int rem = buffer.remaining();
                if (rem == 0) {
                    return; // fake empty packet after client has closed connection
                }
                long n = buffer.getLong();
                // switch to write
                buffer.clear();
                buffer.putLong(n); // flip auto
                write(channel);
                sStartedW.incrementAndGet();
            }

            @Override
            public void writeCompleted(Integer result) {
                sEndedW.incrementAndGet();
                writecount++;
                read(channel);
                sStartedR.incrementAndGet();
            }

            @Override
            public void writeFailed(Throwable exc) {
                sEndedW.incrementAndGet();
                if (!channel.getChannel().isOpen()) {
                    return;
                }
                // TODO Auto-generated method stub
                exc.printStackTrace();
            }

            @Override
            public void readFailed(Throwable exc) {
                sEndedR.incrementAndGet();
                // TODO gracefully shutdown when clien closed connection
                try {
                    channel.close();
                    return;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if (!channel.getChannel().isOpen()) {
                    return;
                }
                exc.printStackTrace();
            }

        }
    }

    class ClientConnection extends AsyncSocketChannel {
        ClientIOR req;
        long numOp;
        MessageSink sink;

        public ClientConnection(InetSocketAddress addr, long numOp, MessageSink sink) throws IOException {
            this.numOp = numOp;
            this.sink = sink;
            connect(addr);
            req=new ClientIOR();
        }

        class ClientIOR extends SocketIORequest {
            AsyncSocketChannel ch=ClientConnection.this;
            int readcount;
            int writecount;

            public ClientIOR() {
                super(4096, true);
                onBuffAdded();
            }

            public void onBuffAdded() {
                if (numOp == 0) {
                    sink.send(null);
                    try {
                        ch.getChannel().close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    return;
                }
                if (numOp < 0) {
                    return;
                }
                numOp--;
                // switch to write
                buffer.clear();
                buffer.putLong(numOp); // flip auto
                write(ch);
                cStartedW.incrementAndGet();
            }

            @Override
            public void writeCompleted(Integer result) {
                cEndedW.incrementAndGet();
                writecount++;
                read(ch);
                cStartedR.incrementAndGet();
            }

            @Override
            public void readCompleted(Integer result) {
                cEndedR.incrementAndGet();
                readcount++;
                int rem = buffer.remaining();
                long n = -1;
                if (rem >= 8) {
                    n = buffer.getLong();
                }
                onBuffAdded();
            }

            @Override
            public void readFailed(Throwable exc) {
                cEndedR.incrementAndGet();
                // TODO Auto-generated method stub
                exc.printStackTrace();
            }

            @Override
            public void writeFailed(Throwable exc) {
                cEndedW.incrementAndGet();
                // TODO Auto-generated method stub
                exc.printStackTrace();
            }

        }
    }

    @Test
    public void testSocketWriteReadThroughput1() throws Exception {
        SimpleExecutorService executor = new SimpleExecutorService();
        testThroughputInternal(executor);
    }

    @Test
    public void testSocketWriteReadThroughput2() throws Exception {
        ThreadFactoryTL tf = new ThreadFactoryTL();
        ExecutorService executor = Executors.newFixedThreadPool(nThreads, tf);
        testThroughputInternal(executor);
    }

    private void testThroughputInternal(ExecutorService executor) throws Exception, IOException, InterruptedException {
        out.println("Using " + executor.getClass().getCanonicalName());
        Task.setCurrentExecutor(executor);
        AsynchronousServerSocketChannel s=AsyncServerSocketChannel.open();
        s.bind(local9999);
        new ServerConnection(s);
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
        s.close();
        time = (System.nanoTime() - start)/1000000000.0f;
        rate = clients*rounds / time;
        out.printf("Elapsed=%f sec; throughput = %f roundtrips/sec \n", time, rate); // actually, round-trips
    }

    public static void main(String[] args) throws Exception {
        AsyncPingTest t=new AsyncPingTest();
        t.testSocketWriteReadThroughput1();
//        t.testSocketWriteReadThroughput2();
    }
}
