
package com.github.rfqu.df4j.ioexample;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;

import org.junit.Test;

import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.core.SimpleExecutorService;
import com.github.rfqu.df4j.core.Task;
import com.github.rfqu.df4j.core.ThreadFactoryTL;
import com.github.rfqu.df4j.core.TwoPortActor;
import com.github.rfqu.df4j.io.AsyncServerSocketChannel;
import com.github.rfqu.df4j.io.AsyncSocketChannel;
import com.github.rfqu.df4j.io.SocketIORequest;
import com.github.rfqu.df4j.util.MessageSink;

class EchoServer {
    AsyncServerSocketChannel assc;
    ServerConnection[] connections;
    
    public EchoServer(InetSocketAddress addr, int connCount) throws IOException {
        assc=new AsyncServerSocketChannel();
        assc.getChannel().bind(addr);
        connections=new ServerConnection[connCount];
        for (int k=0; k<connections.length; k++) {
            ServerConnection connection = new ServerConnection();
            assc.send(connection);
            connections[k]=connection;
       }
    }

    public void close() {
        for (int k=0; k<connections.length; k++) {
            try {
                connections[k].close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try {
            assc.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
}

class ServerConnection extends AsyncSocketChannel {
    SocketIORequest r;
    
    ServerConnection() {
        r=new SocketIORequest(1024, false);
        reader.send(r);
    }

    Actor<SocketIORequest> reader=new Actor<SocketIORequest>() {
        {start();}
        @Override
        protected void act(SocketIORequest emtyBuf) throws Exception {
            //System.out.println("ServerConnection read init id="+emtyBuf.id);
            read(emtyBuf, writer);
        }

        @Override
        protected void complete() throws Exception {
            
        }
    };
    Actor<SocketIORequest> writer=new Actor<SocketIORequest>() {
        boolean stopped=false;
        int num=0;
        {start();}
        
        @Override
        protected void act(SocketIORequest message) throws Exception {
            num++;
            if (stopped) {
                return;
            }
            if (message.getExc()!=null) { // reading failed
                // stop at any error
                stopped=true;
                return;
            }
            //System.out.println("ServerConnection write init id="+message.id);
            // read client's message
            try {
                message.getBuffer().getInt();
            } catch (Exception e) {
                System.err.println("ServerConnection write init id="+message.id+" num="+num);
                e.printStackTrace();
                return;
            }
            // write it back
            write(message, reader);
        }

        @Override
        protected void complete() throws Exception {
            
        }
    };
        
}

class ClientConnection extends AsyncSocketChannel {
    private static final int BUF_SIZE = 1024;
    SocketIORequest[] reguests=new SocketIORequest[2];
    int rounds;
    MessageSink sink;
    int writeCount=0;

    public ClientConnection(InetSocketAddress addr, int rounds, MessageSink sink) throws IOException {
        this.rounds=rounds;
        this.sink=sink;
        super.connect(addr);
        for (int k=0; k<reguests.length; k++) {
            reguests[k]=new SocketIORequest(BUF_SIZE, false);
        }
        writer.send(reguests[0]);
        reader.send(reguests[1]);
    }

    Actor<SocketIORequest> writer=new Actor<SocketIORequest>() {
        @Override
        protected void act(SocketIORequest emtyBuf) throws Exception {
            if (rounds<=0) {
                //System.out.println("ClientConnection finished");
                return;
            }
            //System.out.println("ClientConnection.write");
            // compile client's message
            ByteBuffer buf = emtyBuf.getBuffer();
            buf.putInt(emtyBuf.id);
            // write it to server
            writeCount++;
            System.err.println("ClientConnection.write: writeCount="+writeCount+" rounds="+rounds);
            write(emtyBuf, comp.left);
        }

        @Override
        protected void complete() throws Exception {
            
        }
    };

    Actor<SocketIORequest> reader=new Actor<SocketIORequest>() {
        @Override
        protected void act(SocketIORequest emtyBuf) throws Exception {
            //System.out.println("ClientConnection.read");
            read(emtyBuf, comp.right);
        }

        @Override
        protected void complete() throws Exception {
            
        }
    };
        
    TwoPortActor<SocketIORequest> comp = new TwoPortActor<SocketIORequest>() {
        boolean compFailed=false;

        @Override
        protected void act(SocketIORequest messageL, SocketIORequest messageR) throws Exception {
            //System.out.println("comp.act");
            ByteBuffer bufL = messageL.getBuffer(); // from writer
            ByteBuffer bufR = messageR.getBuffer();  // from reader
            if (!bufL.equals(bufR)) {
                compFailed=true;
            }
            // after compare, end buffers to back to reader and writer
            rounds--;
            if (rounds==0) {
                System.out.println("comp.act: rounds="+rounds+" closing");
                sink.countDown();
                close();
            } else {
                System.out.println("comp.act: round="+rounds);
                writer.send(messageL);
                reader.send(messageR);
            }
        }

        @Override
        protected void complete() throws Exception {
            if (compFailed) {
                System.err.println("comparison failed");
            }
        }
        
    };
}


public class EchoServerTest {
    InetSocketAddress local9999 = new InetSocketAddress("localhost", 9998);
    PrintStream out=System.out;
    PrintStream err=System.err;
    static int numclients=2;//300;
    static int rounds = 1;//100; // per client
    static int nThreads=Runtime.getRuntime().availableProcessors();

    private void testThroughputInternal(ExecutorService executor) throws Exception, IOException, InterruptedException {
        out.println("Using " + executor.getClass().getCanonicalName());
        Task.setCurrentExecutor(executor);
        
        EchoServer es=new EchoServer(local9999, numclients);
        
        MessageSink sink = new MessageSink(numclients);
        ClientConnection[] clients=new ClientConnection[numclients];
        long start0 = System.nanoTime();
        for (int i = 0; i < numclients; i++) {
            clients[i]=new ClientConnection(local9999, rounds, sink);
        }
        
        long start = System.nanoTime();
        float time = (start - start0)/1000000000.0f;
        float rate = numclients / time;
        out.printf("%d clients started in %f sec; rate=%f clients/sec \n", numclients, time, rate); // actually, round-trips
        sink.await();
        es.close();
        out.println("all closed");
        time = (System.nanoTime() - start)/1000000000.0f;
        rate = numclients*rounds / time;
        out.printf("Elapsed=%f sec; throughput = %f roundtrips/sec \n", time, rate); // actually, round-trips
    }

    @Test
    public void testSocketWriteReadThroughput1() throws Exception {
        testThroughputInternal(new SimpleExecutorService());
    }

    @Test
    public void testSocketWriteReadThroughput2() throws Exception {
        testThroughputInternal(ThreadFactoryTL.newFixedThreadPool(nThreads));
    }

    public static void main(String[] args) throws Exception {
        EchoServerTest t=new EchoServerTest();
        t.testSocketWriteReadThroughput1();
        t.testSocketWriteReadThroughput2();
    }
}
