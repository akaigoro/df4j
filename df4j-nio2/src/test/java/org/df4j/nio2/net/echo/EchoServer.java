package org.df4j.nio2.net.echo;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.port.InpFlood;
import org.df4j.core.util.Logger;
import org.df4j.nio2.net.AsyncSocketChannel;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * generates {@link EchoProcessor}s for incoming connections
 *
 */
public class EchoServer extends Actor {
    public static final int BUF_SIZE = 128;
    protected final Logger LOG = new Logger(this, Level.INFO);
    InpFlood<AsyncSocketChannel> inp = new InpFlood<>(this);
    Set<EchoProcessor> echoProcessors = new HashSet<>();
    long connBSerialNum = 0;

    public EchoServer(Dataflow dataflow) throws IOException {
        super(dataflow);
    }

    public void whenComplete() {
        for (EchoProcessor processor: echoProcessors) {
            processor.complete();
        }
    }

    public void whenError() {
        whenComplete();
    }

    @Override
    protected void runAction() throws Throwable {
        if (inp.isCompleted()) {
            complete();
            return;
        }
        AsyncSocketChannel serverConn = inp.remove();
        EchoProcessor processor =
                new EchoProcessor(this, getDataflow(), serverConn, connBSerialNum++); // create client connection
        processor.start();
    }
}
