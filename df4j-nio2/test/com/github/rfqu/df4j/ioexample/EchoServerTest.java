
package com.github.rfqu.df4j.ioexample;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import com.github.rfqu.df4j.core.*;
import com.github.rfqu.df4j.io.*;

public class EchoServerTest {

static class EchoServer {
    AsyncServerSocketChannel assc;
    AsyncSocketChannel[] connections;
    
    public EchoServer(InetSocketAddress addr, int connCount) throws IOException {
        assc=new AsyncServerSocketChannel();
        assc.getChannel().bind(addr);
        connections=new AsyncSocketChannel[connCount];
        for (int k=0; k<connections.length; k++) {
        	AsyncSocketChannel connection = new AsyncSocketChannel();
        	connection.connect(assc);
            ServerRequest request=new ServerRequest(1024, false);
            connection.reader.send(request);
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

static class ServerRequest extends SocketIORequest {

	public ServerRequest(int capacity, boolean direct) {
		super(capacity, direct);
	}

	public ServerRequest(ByteBuffer buf) {
		super(buf);
	}

	@Override
	public void readCompleted(Integer result, AsyncSocketChannel channel) {
	    assert(true);
//       System.out.println("  ServerRequest readCompleted id="+id);
        // read client's message
        try {
            // as if all the data read and written back
            buffer.position(buffer.limit());
            // write it back
            channel.writer.send(this);
        } catch (Exception e) {
            System.err.println("  ServerRequest write id="+id);
            e.printStackTrace();
        }
	}

	@Override
	public void readFailed(Throwable exc, AsyncSocketChannel channel) {
	    if (exc instanceof AsynchronousCloseException) {
	        return; // user closed connection
	    }
        System.err.println("  ServerRequest read failed id="+id+":");
        exc.printStackTrace();
        System.err.println("  t n="+Thread.currentThread().getName());
	}

	@Override
	public void writeCompleted(Integer result, AsyncSocketChannel channel) {
	    //           System.out.println("  ServerRequest writeCompleted id="+id);
        channel.reader.send(this);
	}

	@Override
	public void writeFailed(Throwable exc, AsyncSocketChannel channel) {
	}

}

static class ClientConnection extends AsyncSocketChannel {
    static final int PERIOD = 300000000; // 300 ms
    private static final int BUF_SIZE = 1024;
    ClientRequest request;
    AtomicLong rounds;
    Aggregator sink;
    int writeCount=0;
    ScheduledThreadPoolExecutor timer;

    public ClientConnection(InetSocketAddress addr, int rounds, Aggregator sink, ScheduledThreadPoolExecutor timer) throws IOException {
        this.rounds=new AtomicLong(rounds);
        this.sink=sink;
        this.timer=timer;
        super.connect(addr);
        request = new ClientRequest(BUF_SIZE, false);
        request.start();
    }

    class ClientRequest extends SocketIORequest implements Runnable {
        Random rand=new Random();
        int data;
        long startTime;
        long sum=0;
        long count=0;
        AsyncSocketChannel channel=ClientConnection.this;
        
        public ClientRequest(int capacity, boolean direct) {
            super(capacity, direct);
        }

        public ClientRequest(ByteBuffer buf) {
            super(buf);
        }

        void start() {
            startTime = System.nanoTime();
            data=rand.nextInt();
            buffer.clear();
            buffer.putInt(data);
//            System.out.println("ClientRequest started id="+id);
            channel.writer.send(this);
        }
        
        @Override
        public void writeCompleted(Integer result, AsyncSocketChannel channel) {
//          System.out.println("ClientRequest writeCompleted res:"+result+" id="+id+" read started");
            channel.reader.send(this);
        }

        @Override
        public void writeFailed(Throwable exc, AsyncSocketChannel channel) {
//            System.err.println("ClientRequest writeFailed id="+id);
            exc.printStackTrace();
        }

        @Override
        public void readCompleted(Integer result, AsyncSocketChannel channel) {
            //System.out.println("ServerConnection write init id="+message.id);
            // read client's message
            try {
//                    System.out.println("ClientRequest readCompleted count="+count+"  id="+id+" res="+result);
                int dataFromServer=getBuffer().getInt();
                if (dataFromServer!=data) {
                    System.out.println("written: "+data+"; read:"+dataFromServer);
                }
                long currentTime = System.nanoTime();
                sum+=(currentTime-startTime);
                count++;
                rounds.decrementAndGet();
                if (rounds.get()==0) {
//                    System.out.println("ClientRequest finished id="+id);
                    sink.send(new Value(((double)sum)/count));
                    return;
                }
                long delay=startTime+PERIOD-currentTime;
                if (delay>1000) {
                    timer.schedule(this, delay, TimeUnit.NANOSECONDS);
                } else {
                    // write it back immediately
                    start();
                }
            } catch (Exception e) {
//                System.err.println("ClientRequest write err: id="+id+":");
                e.printStackTrace();
            }
        }

        @Override
        public void readFailed(Throwable exc, AsyncSocketChannel channel) {
//            System.err.println("ClientRequest readFailed id="+id+":");
            exc.printStackTrace();
        }

        // for timer
        @Override
        public void run() {
            start();
        }
        
    }

}

/**
 * computes average of input values
 */
static class Aggregator extends Actor<Value> {
    int numclients;
    long sum=0;
    long counter=0;
    PortFuture<Double> avg=new PortFuture<Double>();
    {super.start();}

    public Aggregator(int numclients) {
        this.numclients=numclients;
    }

    @Override
    protected void act(Value message) throws Exception {
        counter++;
        sum+=message.value;
        if (counter==numclients) {
            complete();
        }
    }
    
    @Override
    protected void complete() throws Exception {
        avg.send(((double)sum)/counter);
    }

}

static class Value extends Link {
    double value;

    public Value(double value) {
        this.value = value;
    }
    
}
    InetSocketAddress local9999 = new InetSocketAddress("localhost", 9998);
    PrintStream out=System.out;
    PrintStream err=System.err;
    static int numclients=1000;
    static int rounds = 20; // per client
    static int nThreads=Runtime.getRuntime().availableProcessors();

    @Test
    public void testThroughput() throws Exception, IOException, InterruptedException {
        Task.setCurrentExecutor(new SimpleExecutorService());
        EchoServer es=new EchoServer(local9999, numclients);
        
        Task.setCurrentExecutor(new SimpleExecutorService());
        Aggregator sink = new Aggregator(numclients);
        ScheduledThreadPoolExecutor timer=new ScheduledThreadPoolExecutor(1);

        ClientConnection[] clients=new ClientConnection[numclients];
        long start = System.nanoTime();
        for (int i = 0; i < numclients; i++) {
            clients[i]=new ClientConnection(local9999, rounds, sink, timer);
        }
        double avgLatency=sink.avg.get();
        es.close();
        out.println("all closed");
        float time = (System.nanoTime() - start)/1000000000.0f;
        float rate = numclients*rounds / time;
        out.printf("Elapsed=%f sec; throughput = %f roundtrips/sec \n", time, rate);
        out.printf("Latency=%f msec \n", avgLatency/1000000);
    }

    public static void main(String[] args) throws Exception {
        EchoServerTest t=new EchoServerTest();
        t.testThroughput();
    }
}
