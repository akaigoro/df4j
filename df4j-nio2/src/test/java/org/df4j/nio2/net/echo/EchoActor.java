package org.df4j.nio2.net.echo;

import org.df4j.core.connector.messagescalar.ScalarCollector;
import org.df4j.nio2.net.BaseServerConnection;
import org.df4j.nio2.net.BuffProcessor;

import java.nio.ByteBuffer;

public class EchoActor extends BaseServerConnection {
    EchoProcessor echoProcessor = new EchoProcessor();

    public EchoActor(ScalarCollector backport) {
        super(backport);
        reader.subscribe(echoProcessor).subscribe(writer).subscribe(reader);
        reader.injectBuffers(4, 128);
    }

    class EchoProcessor extends BuffProcessor {

        @Override
        protected void act() throws Exception {
            ByteBuffer buf = input.current();
            output.post(buf);
        }
    }
}
