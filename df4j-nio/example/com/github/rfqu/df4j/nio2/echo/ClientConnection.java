package com.github.rfqu.df4j.nio2.echo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.EventSource;
import com.github.rfqu.df4j.core.Timer;
import com.github.rfqu.df4j.nio.AsyncSocketChannel;
import com.github.rfqu.df4j.nio.SocketIORequest;
import com.github.rfqu.df4j.testutil.DoubleValue;

class ClientConnection
   implements EventSource<SocketChannel, Callback<SocketChannel>>
{
    static final long timeout=1000;// ms
    static AtomicInteger ids=new AtomicInteger(); // DEBUG

    private EchoServerGlobTest echoServerTest;
    private final Timer timer;
    public int id=EchoServerGlobTest.ids.addAndGet(1);
    public int serverId;
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

    @Override
	public ClientConnection addListener(Callback<SocketChannel> listener) {
		channel.addListener(listener);
		return this;
	}

    /** starts write operation
     */
	IOHandler<CliRequest> startWrite = new IOHandler<CliRequest>() {
        @Override
		public void completed(int result, CliRequest request) {// throws Exception {
            counterRun++;
            request.start = System.currentTimeMillis();
            request.data = rand.nextInt();
            ByteBuffer buffer = request.getBuffer();
            buffer.clear();
            buffer.putInt(request.data);
            channel.write(request, endWrite, timeout);
        }
    };

    IOHandler<CliRequest> endWrite = new IOHandler<CliRequest>() {
        @Override
        public void completed(int result, CliRequest request) {//throws ClosedChannelException {
//            System.err.println("  client Request write ended, id="+id+" rid="+request.rid);
            channel.read(request, endRead, timeout);
//            System.err.println("client Request read started id="+id+" rid="+request.rid);
        }

        @Override
        public void timedOut(CliRequest request) {
            System.err.println("endWrite.timedOut!");
            Thread.dumpStack();
        }       
    };
    
    IOHandler<CliRequest> endRead = new IOHandler<CliRequest>() {
        @Override
        public void completed(int result, CliRequest request) {
//            System.err.println("  client Request read ended; id="+id+" rid="+request.rid+" count="+count);
            // read client's message
            request.checkData();
            long currentTime = System.currentTimeMillis();
            sum+=(currentTime-request.start);
            count++;
            rounds.decrementAndGet();
            if (rounds.get()==0) {
//                System.out.println("SocketIORequest finished id="+id);
                channel.close();
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
        public void timedOut(CliRequest request) {
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
}