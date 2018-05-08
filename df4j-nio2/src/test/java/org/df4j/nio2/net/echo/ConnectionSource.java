package org.df4j.nio2.net.echo;

import org.df4j.core.connector.messagescalar.ScalarCollector;
import org.df4j.core.connector.permitstream.Semafor;
import org.df4j.core.connector.reactivestream.Publisher;
import org.df4j.core.connector.reactivestream.ReactiveOutput;
import org.df4j.core.connector.reactivestream.Subscriber;
import org.df4j.core.node.Actor;
import org.df4j.nio2.net.BaseServerConnection;

public class ConnectionSource extends Actor implements Publisher<BaseServerConnection>, ScalarCollector<BaseServerConnection> {
    Semafor allowedConnections = new Semafor(this);
    ReactiveOutput<BaseServerConnection> output = new ReactiveOutput<>(this);

    public ConnectionSource(int connCount) {
        allowedConnections.release(connCount);
    }

    @Override
    public <S extends Subscriber<? super BaseServerConnection>> S subscribe(S subscriber) {
        output.subscribe(subscriber);
        return subscriber;
    }

    @Override
    public void post(BaseServerConnection message) {
        allowedConnections.release();
    }

    @Override
    protected void act() {
        BaseServerConnection conn = new EchoActor(this);
        output.post(conn);
    }
}
