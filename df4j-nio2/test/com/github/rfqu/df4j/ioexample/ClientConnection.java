package com.github.rfqu.df4j.ioexample;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.github.rfqu.df4j.core.SerialExecutor;
import com.github.rfqu.df4j.core.Timer;
import com.github.rfqu.df4j.ioexample.EchoServerTest.Aggregator;
import com.github.rfqu.df4j.nio2.AsyncSocketChannel;
import com.github.rfqu.df4j.nio2.SocketIOHandler;
import com.github.rfqu.df4j.nio2.SocketIORequest;
import com.github.rfqu.df4j.util.DoubleValue;

class ClientConnection implements Runnable, Comparable<ClientConnection> {
    static final long timeout=1000;// ms
    static AtomicInteger ids=new AtomicInteger(); // DEBUG

    private EchoServerTest echoServerTest;
    private final Timer timer;
    public int id=EchoServerTest.ids.addAndGet(1);
    SerialExecutor serex=new SerialExecutor();
    AsyncSocketChannel channel=new AsyncSocketChannel();
    Request request;

    AtomicLong rounds;
    Aggregator sink;

    Random rand=new Random();
    long sum=0;
    long count=0;
    int counterRun=0;

    public ClientConnection(EchoServerTest echoServerTest, InetSocketAddress addr, int rounds, Aggregator sink) throws IOException {
        this.echoServerTest = echoServerTest;
        this.timer = echoServerTest.timer;
        this.rounds=new AtomicLong(rounds);
        this.sink=sink;
        channel.connect(addr);
        run();
    }

    /** starts write operation
     */
    public void run() {
        counterRun++;
        ByteBuffer buffer = ByteBuffer.allocate(EchoServerTest.BUF_SIZE);
        request=new Request(buffer);
        request.start = System.currentTimeMillis();
        request.data = rand.nextInt();
        buffer.clear();
        buffer.putInt(request.data);
        try {
            channel.write(request, endWrite, timeout);
//            System.err.println("client Request write started id=" + id + " rid=" + request.rid);
        } catch (ClosedChannelException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    SocketIOHandler endWrite = new SocketIOHandler(serex) {

        @Override
        protected void completed(Integer result, SocketIORequest request) throws ClosedChannelException {
//            System.err.println("  client Request write ended, id="+id+" rid="+request.rid);
            channel.read(request, endRead, timeout);
//            System.err.println("client Request read started id="+id+" rid="+request.rid);
        }

        @Override
        protected void timedOut(SocketIORequest request) {
            System.err.println("endWrite.timedOut!");
            Thread.dumpStack();
        }
        
    };
    
    SocketIOHandler endRead = new SocketIOHandler(serex) {

        @Override
        protected void completed(Integer result, SocketIORequest request0) {
//            System.err.println("  client Request read ended; id="+id+" rid="+request.rid+" count="+count);
            // read client's message
            Request request=(Request) request0;
            request.checkData();
            long currentTime = System.currentTimeMillis();
            sum+=(currentTime-request.start);
            count++;
            rounds.decrementAndGet();
            if (rounds.get()==0) {
//                System.out.println("SocketIORequest finished id="+id);
                sink.send(new DoubleValue(((double)sum)/count));
                echoServerTest.clients.remove(ClientConnection.this);
//                System.out.println("clients="+echoServerTest.clients.size());
                return;
            }
            long targetTime=request.start+EchoServerTest.PERIOD;
            if (targetTime>currentTime) {
                timer.scheduleAt(ClientConnection.this, targetTime);
            } else {
                // write it back immediately
                ClientConnection.this.run();
            }
        }

        @Override
        protected void timedOut(SocketIORequest request) {
            System.err.println("endRead.timedOut!");
            Thread.dumpStack();
        }
    };

    static class Request extends SocketIORequest {
        long start;
        int data;

        public Request(ByteBuffer buf) {
            super(buf);
        }

        void checkData() {
            int dataFromServer;
            try {
                dataFromServer = getBuffer().getInt();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return;
            }
            if (dataFromServer!=data) {
                System.err.println("written: "+data+"; read:"+dataFromServer);
                return;
            }
        }

    }

    @Override
    public int compareTo(ClientConnection o) {
        return id-o.id;
    }
}