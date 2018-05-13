package org.df4j.nio2.net.echo;

import org.df4j.core.connector.messagescalar.ScalarCollector;
import org.df4j.nio2.net.ServerConnection;
import org.df4j.nio2.net.BuffProcessor;

import java.nio.ByteBuffer;

public class EchoServerConnection extends ServerConnection {
    EchoProcessor echoProcessor = new EchoProcessor();

    public EchoServerConnection(ScalarCollector backport) {
        super(backport);
        reader.subscribe(echoProcessor).subscribe(writer).subscribe(reader);
        reader.injectBuffers(1, 128);
        echoProcessor.start();
        LOG.config(getClass().getName()+" created");
    }

    class EchoProcessor extends BuffProcessor {

        @Override
        protected void act() throws Exception {
            ByteBuffer buf = input.current();
            output.post(buf);
        }
    }
}
