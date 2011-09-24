
package com.github.rfqu.df4j.ioexample;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

import com.github.rfqu.df4j.core.Promise;
import com.github.rfqu.df4j.core.SimpleExecutorService;
import com.github.rfqu.df4j.core.Task;
import com.github.rfqu.df4j.core.ThreadFactoryTL;
import com.github.rfqu.df4j.io.AsyncServerSocketChannel;
import com.github.rfqu.df4j.io.AsyncSocketChannel;
import com.github.rfqu.df4j.io.SocketIORequest;

/**
 * Requires Java 7.
 */
public class AsyncPingTest {
    static PrintStream out=System.out;
    private static final int NB = 2;
    int rounds = 1000*10;
    int nThreads=Runtime.getRuntime().availableProcessors();
    InetSocketAddress local9999 = new InetSocketAddress("localhost", 9998);

    static class Server extends AsyncServerSocketChannel {
        ServerConnection asc;
        
        Server(InetSocketAddress addr) throws Exception{
            getChannel().bind(addr);
            start();
        }
        
        @Override
        public void accepted(AsynchronousSocketChannel ch) {
            asc=new ServerConnection(ch);
            try {
                getChannel().close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
    }
    
    static class ServerConnection extends AsyncSocketChannel {
        int readcount;
        int writecount;

        public ServerConnection(AsynchronousSocketChannel ch) {
            super(ch);
            for (int k=0; k<NB; k++) {
                onBuffAdded(new SocketIORequest(ByteBuffer.allocateDirect(4096)));
            }
        }

        public void onBuffAdded(SocketIORequest r) {
            read(r);
        }
        
        @Override
        public void onReadCompleted(SocketIORequest request) {
            readcount++;
            ByteBuffer buffer = request.getBuffer();
            int rem =buffer.remaining();
            if (rem==0) {
                return; // fake empty packet after client has closed connection
            }
            long n=buffer.getLong();
            // out.println("    server readcount="+readcount+" rem="+rem+" n="+n);
            // switch to write
            buffer.clear();
            buffer.putLong(n); // flip auto
            write(request); 
        }

        @Override
        public void onWriteCompleted(SocketIORequest request) {
            writecount++;
            // out.println("    server writecount="+writecount);
            onBuffAdded(request);
        }

        @Override
        public void onWriteFailed(Throwable exc, SocketIORequest request) {
            if (!channel.isOpen()) {
                out.println("    server channel closed");
                return;
            }
            // TODO Auto-generated method stub   
            exc.printStackTrace();
        }
        
        @Override
        public void onReadFailed(Throwable exc, SocketIORequest request) {
            if (!channel.isOpen()) {
                out.println("    server channel closed");
                return;
            }
            // TODO Auto-generated method stub            
            exc.printStackTrace();
        }

    }
    
    static class ClientConnection extends AsyncSocketChannel {
        int readcount;
        int writecount;
        long numOp;
        Promise<Integer> sink = new Promise<Integer>();

        public ClientConnection(InetSocketAddress addr, long numOp) throws IOException {
            this.numOp=numOp;
            connect(addr);
            for (int k=0; k<NB; k++) {
                onBuffAdded(new SocketIORequest(ByteBuffer.allocateDirect(4096)));
            }
        }

        public void onBuffAdded(SocketIORequest request) {
            if (numOp==0) {
                sink.send(0);
                // out.println("client close; readcount="+readcount+" writecount="+writecount);
                try {
                    channel.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return;
            }
            if (numOp<0) {
                // out.println("!!! client write started numOp ="+numOp+"!!! writecount="+writecount);
                return;
            }
            numOp--;
            // out.println("client write started numOp ="+numOp+" writecount="+writecount);
            // switch to write
            ByteBuffer buffer = request.getBuffer();
            buffer.clear();
            buffer.putLong(numOp); // flip auto
            write(request);
        }

        @Override
        public void onWriteCompleted(SocketIORequest request) {
            writecount++;
            // out.println("client write ended, read started: numOp ="+numOp+" writecount="+writecount);
            read(request);
        }

        @Override
        public void onReadCompleted(SocketIORequest request) {
            readcount++;
            ByteBuffer buffer = request.getBuffer();
            int rem =buffer.remaining();
            long n=-1;
            if (rem>=8) {
                n=buffer.getLong();
            }
            // out.println("client read ended numOp ="+numOp+" readcount="+readcount+" rem="+rem+" n="+n);
            onBuffAdded(request);
        }

        @Override
        public void onReadFailed(Throwable exc, SocketIORequest request) {
            // TODO Auto-generated method stub            
            exc.printStackTrace();
        }

        @Override
        public void onWriteFailed(Throwable exc, SocketIORequest request) {
            // TODO Auto-generated method stub
            exc.printStackTrace();            
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
        Server s=new Server(local9999);
        ClientConnection cli=new ClientConnection(local9999, rounds);
        
        long start = System.nanoTime();
        cli.sink.get();
        float time = (System.nanoTime() - start)/1000000000.0f;
        float rate = rounds / time;
        ServerConnection asc = s.asc;
        out.printf("numOp=%d  readcount=%d writecount=%d\n", cli.numOp, asc.readcount, asc.writecount);
        out.printf("Elapsed=%f sec; throughput = %f packets/sec \n", time, rate); // actually, round-trips
    }
}
