package com.github.rfqu.df4j.nio2.echo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.SerialExecutor;
import com.github.rfqu.df4j.ext.Timer;
import com.github.rfqu.df4j.nio2.AsyncSocketChannel;
import com.github.rfqu.df4j.nio2.SocketIOHandler;
import com.github.rfqu.df4j.nio2.SocketIORequest;
import com.github.rfqu.df4j.util.DoubleValue;

class ClientConnection implements Comparable<ClientConnection> {
    static final long timeout=1000;// ms
    static AtomicInteger ids=new AtomicInteger(); // DEBUG

    private EchoServerGlobTest echoServerTest;
    private final Timer timer;
    public int id=EchoServerGlobTest.ids.addAndGet(1);
    public int serverId;
    SerialExecutor serex=new SerialExecutor();
    AsyncSocketChannel channel;
    CliRequest request;
    AtomicLong rounds;
    Random rand=new Random();
    long sum=0;
    long count=0;
    int counterRun=0;

    public ClientConnection(EchoServerGlobTest echoServerTest, InetSocketAddress addr, int rounds) throws IOException {
        this.echoServerTest = echoServerTest;
        this.timer = echoServerTest.timer;
        this.rounds=new AtomicLong(rounds);
        channel=new AsyncSocketChannel(addr);
        ByteBuffer buffer = ByteBuffer.allocate(EchoServerGlobTest.BUF_SIZE);
        request=new CliRequest(buffer);
//        channel.read(request, endRead1, timeout);
        request.setResult(0);
        startWrite.send(request);
    }

	public void addConnectListener(Callback<AsynchronousSocketChannel> listener) {
		channel.addConnectListener(listener);
	}

    /** starts write operation
     */
    SocketIOHandler<CliRequest> startWrite = new SocketIOHandler<CliRequest>(serex) {
        @Override
        protected void completed(int result, CliRequest request) throws Exception {
            counterRun++;
            request.start = System.currentTimeMillis();
            request.data = rand.nextInt();
            ByteBuffer buffer = request.getBuffer();
            buffer.clear();
            buffer.putInt(request.data);
            channel.write(request, endWrite, timeout);
        }
    };

    SocketIOHandler<CliRequest> endWrite = new SocketIOHandler<CliRequest>(serex) {
        @Override
        protected void completed(int result, CliRequest request) throws ClosedChannelException {
//            System.err.println("  client Request write ended, id="+id+" rid="+request.rid);
            channel.read(request, endRead, timeout);
//            System.err.println("client Request read started id="+id+" rid="+request.rid);
        }

        @Override
        protected void timedOut(CliRequest request) {
            System.err.println("endWrite.timedOut!");
            Thread.dumpStack();
        }       
    };
    
    SocketIOHandler<CliRequest> endRead = new SocketIOHandler<CliRequest>(serex) {
        @Override
        protected void completed(int result, CliRequest request) {
//            System.err.println("  client Request read ended; id="+id+" rid="+request.rid+" count="+count);
            // read client's message
            request.checkData();
            long currentTime = System.currentTimeMillis();
            sum+=(currentTime-request.start);
            count++;
            rounds.decrementAndGet();
            if (rounds.get()==0) {
//                System.out.println("SocketIORequest finished id="+id);
                try {
                    channel.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                DoubleValue avg = new DoubleValue(((double)sum)/count);
                echoServerTest.clientFinished(ClientConnection.this, avg);
//                System.out.println("clients="+echoServerTest.clients.size());
                return;
            }
            long targetTime=request.start+EchoServerGlobTest.PERIOD;
            if (targetTime>currentTime) {
//            if (false) {
                timer.schedule(startWrite, request, EchoServerGlobTest.PERIOD);
            } else {
                // write it back immediately
                startWrite.send(request);
            }
        }

        @Override
        protected void timedOut(CliRequest request) {
            System.err.println("endRead.timedOut!");
            Thread.dumpStack();
        }
    };

    static class CliRequest extends SocketIORequest<CliRequest> {
        long start;
        int data;

        public CliRequest(ByteBuffer buf) {
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