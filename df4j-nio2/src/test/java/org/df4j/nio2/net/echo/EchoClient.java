package org.df4j.nio2.net.echo;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.AsyncProc;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.port.InpFlow;
import org.df4j.core.port.InpScalar;
import org.df4j.core.port.OutFlow;
import org.df4j.core.util.Logger;
import org.df4j.nio2.net.AsyncClientSocketChannel;
import org.df4j.nio2.net.AsyncSocketChannel;
import org.junit.Assert;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.Charset;

/**
 * sends and receives limited number of messages
 */
class EchoClient extends AsyncProc {
    protected static final Logger LOG = Logger.getLogger(Speaker.class.getName());
    static final Charset charset = Charset.forName("UTF-16");
    private final int total;

    public static ByteBuffer toByteBuf(String message) {
        return ByteBuffer.wrap(message.getBytes(charset));
    }

    public static String fromByteBuf(ByteBuffer b) {
        return new String(b.array(), charset);
    }

    protected InpScalar<AsynchronousSocketChannel> inp = new InpScalar<>(this);

    public EchoClient(Dataflow dataflow, SocketAddress addr, int total) throws IOException {
        super(dataflow);
        this.total = total;
        AsyncClientSocketChannel clientStarter = new AsyncClientSocketChannel(addr);
        clientStarter.subscribe(inp);
    }

    AsyncSocketChannel clientConn;

    public void runAction() {
        AsynchronousSocketChannel assc = inp.current();
        Dataflow dataflow = getDataflow();
        clientConn = new AsyncSocketChannel(dataflow, assc);
        dataflow.leave();
        clientConn.setName("client");
        Speaker speaker = new Speaker(this.dataflow);
        speaker.start();
        LOG.info("Speaker started");
        this.dataflow.leave();
    }

    class Speaker extends Actor {
        private int count;
        OutFlow<ByteBuffer> buffers2write = new OutFlow<>(this);
        OutFlow<String> sentMsgs = new OutFlow<>(this, 10);

        public Speaker(Dataflow dataflow) {
            super(dataflow);
            this.count = total;
            buffers2write.subscribe(clientConn.writer.input);
            clientConn.writer.output.subscribe(clientConn.reader.input);
            Listener listener = new Listener(dataflow);
            sentMsgs.subscribe(listener.sentMsgs);
            listener.start();
        }

        public void runAction() {
            String message = "hi there "+count;
            ByteBuffer buf = toByteBuf(message);
            buffers2write.onNext(buf);
            sentMsgs.onNext(message);
            LOG.info("Speaker sent message: "+message);
            count--;
            if (count == 0) {
                LOG.info("Speaker finished successfully");
                stop();
            }
        }
    }

    class Listener extends Actor {
        private int count;
        InpFlow<String> sentMsgs = new InpFlow<>(this);
        InpFlow<ByteBuffer> readBuffers = new InpFlow<>(this);

        public Listener(Dataflow dataflow) {
            super(dataflow);
            this.count = total;
            clientConn.reader.output.subscribe(readBuffers);
        }

        public void runAction() {
            String sent = sentMsgs.remove();
            ByteBuffer received = readBuffers.remove();
            String m2 = fromByteBuf(received);
            LOG.info("Listener received message:"+m2);
            Assert.assertEquals(sent, m2);
            count--;
            if (count == 0) {
                LOG.info("Listener finished successfully");
                stop();
            }
        }

    }

}
