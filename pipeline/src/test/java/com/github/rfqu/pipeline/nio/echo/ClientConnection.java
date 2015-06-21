package com.github.rfqu.pipeline.nio.echo;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.df4j.pipeline.core.BoltBase;
import org.df4j.pipeline.core.Pipeline;
import org.df4j.pipeline.core.Transformer;
import org.df4j.pipeline.df4j.core.Port;
import org.df4j.pipeline.df4j.core.StreamPort;
import org.df4j.pipeline.df4j.testutil.DoubleValue;
import org.df4j.pipeline.io.net.AsyncSocketChannel;

/**
 * Implements a client of Echo-server.
 * Sends messages and compare replies.
 * 
 * @author kaigorodov
 *
 */
class ClientConnection extends Pipeline {

    static final long timeout = 1000;// ms
    static AtomicInteger ids = new AtomicInteger(); // DEBUG
    public static final int BUF_SIZE = 8;

    public int id = EchoClient.ids.addAndGet(1);
    Client client=new Client();
    AtomicInteger rounds;

    public ClientConnection(InetSocketAddress addr, int rounds) throws IOException {
        AsyncSocketChannel channel=new AsyncSocketChannel(addr);
        this.rounds=new AtomicInteger(rounds);
        setSource(channel.reader)
          .addTransformer(client)
          .setSink(channel.writer);
    }
   
    class Client extends BoltBase implements Transformer<ByteBuffer, ByteBuffer> {
        Random rand = new Random();
        long sum = 0;
        int count1startWrite = 0;
        int count2endWrite = 0;
        int count3endRead = 0;
        long writeTime;
        long lastMessage;

        // ----------------- Source part

        /** there output messages go */
        protected StreamPort<ByteBuffer> sinkPort;

        @Override
        public void setSinkPort(StreamPort<ByteBuffer> sinkPort) {
            this.sinkPort = sinkPort;
        }

        /**
         * here buffers after writing return shorted to reader's return port
         */
        @Override
        public Port<ByteBuffer> getReturnPort() {
            return returnPort;
        }

        // ----------------- Sink part

        /** here replies from server arrive */
        protected StreamPort<ByteBuffer> myInput = new StreamPort<ByteBuffer>() {

            @Override
            public void post(ByteBuffer buf) {
                long value = buf.getLong();
                assertEquals(lastMessage, value);
                count3endRead++;
                // System.err.println("  client Request read ended; id="+id+" rid="+request.rid+" count="+count);
                long currentTime = System.currentTimeMillis();
                sum += (currentTime - writeTime);
                long r = rounds.decrementAndGet();
                if (r == 0) {
                    // System.out.println("SocketByteBufferRequest finished id="+id);
                    DoubleValue avg = new DoubleValue(((double) sum) / count3endRead);
                    ClientConnection.this.post(avg);
                    // System.out.println("clients="+echoServerTest.clients.size());
                    return;
                }
                write(rand.nextLong(), buf);
            }

            @Override
            public void close() {
            }

            @Override
            public boolean isClosed() {
                return false;
            }

        };

        /** here input messages arrive */
        @Override
        public StreamPort<ByteBuffer> getInputPort() {
            return myInput;
        }

        /** there input messages return */
        protected Port<ByteBuffer> returnPort;

        @Override
        public void setReturnPort(Port<ByteBuffer> returnPort) {
            this.returnPort = returnPort;
        }

        // ---------- glue part

        public void close() {
            myInput.close();
        }

        public void start() {
            // single buffer is enough
            ByteBuffer buf = ByteBuffer.allocate(BUF_SIZE);
            write(rand.nextLong(), buf);
        }

        public void write(long value, ByteBuffer buf) {
            count1startWrite++;
            buf.clear();
            buf.putLong(value);
            buf.flip();
            lastMessage = value;
            writeTime = System.currentTimeMillis();
            sinkPort.post(buf);
        }

    }

}