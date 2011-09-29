
package com.github.rfqu.df4j.ioexample;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import org.junit.Before;
import org.junit.Test;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

import com.github.rfqu.df4j.core.SimpleExecutorService;
import com.github.rfqu.df4j.core.Task;
import com.github.rfqu.df4j.io.AsyncServerSocketChannel;
import com.github.rfqu.df4j.io.AsyncSocketChannel;
import com.github.rfqu.df4j.io.SocketIORequest;
import com.github.rfqu.df4j.util.MessageSink;

/**
 * Requires Java 7.
 */
public class AsyncPingTest {
    private static final Logger log = Logger.getLogger( AsyncPingTest.class ); 
    SimpleExecutorService executor;
    InetSocketAddress local9999;
    PrintStream out=System.out;
    PrintStream err=System.err;
    int clients=2;
    int rounds = 4; // per client

    @Before
    public void init() {
        executor = new SimpleExecutorService();
        out.println("Using " + executor.getClass().getCanonicalName());
        local9999 = new InetSocketAddress("localhost", 9998);
        log.addAppender(new ConsoleAppender(new SimpleLayout()));
        log.setLevel(Level.ALL);
    }
    
    @Test
    public void testServerSocket() throws Exception {
        Task.setCurrentExecutor(executor);
 //       AsyncServerSocketChannel s=new AsyncServerSocketChannel(local9999);
//        ServerConnection sc = new ServerConnection(s);

        MessageSink sink = new MessageSink(1);
        ClientConnection cc = new ClientConnection(local9999, 1, sink);
        sink.await();
    }

    //@Test
    public void testSocketWriteReadThroughput() throws Exception {
        Task.setCurrentExecutor(executor);
        AsyncServerSocketChannel s=new AsyncServerSocketChannel(local9999);
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

    class ServerConnection extends AsyncSocketChannel {
        ServerIOR request=new ServerIOR();
        
        public ServerConnection(AsyncServerSocketChannel s) throws IOException {
            super.connect(s);
            read(request);
            log.debug("    server read started");
        }

        @Override
        protected void requestCompleted(SocketIORequest request) {
            super.requestCompleted(request);
            
        }

        class ServerIOR extends SocketIORequest {
            int readcount;
            int writecount;

            public ServerIOR() {
                super(4096, true);
                this.channel = ServerConnection.this;
            }

            @Override
            protected void readCompleted(Integer result) {
                readcount++;
                int rem = buffer.remaining();
                long n = buffer.getLong();
                log.debug("    server readcount="+readcount+" rem="+rem+" n="+n+" res="+result);
                if (result == -1) {
                    return; // client has closed connection
                }
                // switch to write
                buffer.clear();
                buffer.putLong(n); // flip auto
                channel.write(this);
            }

            @Override
            protected void writeCompleted(Integer result) {
                writecount++;
                 log.debug("    server writecount="+writecount);
                 channel.read(this);
            }

            @Override
            protected void writeFailed(Throwable exc) {
                if (!channel.getChannel().isOpen()) {
                    log.debug("    server channel closed");
                    return;
                }
                // TODO Auto-generated method stub
                log.debug("    ServerIOR writeFailed:");
                exc.printStackTrace();
            }

            @Override
            protected void readFailed(Throwable exc) {
                // TODO gracefully shutdown when client closed connection
                try {
                    channel.close();
                    return;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    log.debug("    ServerIOR.channel.close failed:");
                    e.printStackTrace();
                }
                if (!channel.getChannel().isOpen()) {
                    log.debug("    server channel closed");
                    return;
                }
                log.debug("    ServerIOR readFailed:");
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

            protected void onBuffAdded() {
                if (numOp == 0) {
                    sink.send(null);
                     log.debug("client close; readcount="+readcount+" writecount="+writecount);
                    try {
                        ch.getChannel().close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    return;
                }
                if (numOp < 0) {
                     log.debug("!!! client write started numOp ="+numOp+"!!! writecount="+writecount);
                    return;
                }
                numOp--;
                 log.debug("client write started numOp ="+numOp+" writecount="+writecount);
                // switch to write
                buffer.clear();
                buffer.putLong(numOp); // flip auto
                ch.write(this);
            }

            @Override
            protected void writeCompleted(Integer result) {
                writecount++;
                 log.debug("client write ended, read started: numOp ="+numOp+" writecount="+writecount);
                ch.read(this);
            }

            @Override
            protected void readCompleted(Integer result) {
                readcount++;
                int rem = buffer.remaining();
                long n = -1;
                if (rem >= 8) {
                    n = buffer.getLong();
                }
                log.debug("client read ended numOp ="+numOp+" readcount="+readcount+" rem="+rem+" n="+n);
                onBuffAdded();
            }

            @Override
            protected void readFailed(Throwable exc) {
                // TODO Auto-generated method stub
                log.debug("    ClientIOR readFailed:");
                exc.printStackTrace();
            }

            @Override
            protected void writeFailed(Throwable exc) {
                // TODO Auto-generated method stub
                log.debug("    ClientIOR writeFailed:");
                exc.printStackTrace();
            }

        }
    }

    public static void main(String[] args) throws Exception {
        AsyncPingTest t=new AsyncPingTest();
        t.init();
        t.testServerSocket();
    }
}
