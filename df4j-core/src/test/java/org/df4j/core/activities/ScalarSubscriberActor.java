package org.df4j.core.activities;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.port.InpScalar;
import org.df4j.core.util.Logger;

import java.util.logging.Level;

public class ScalarSubscriberActor extends Actor {
    protected final Logger logger = new Logger(this);
    final int delay;
    PublisherActor[] pubs;
    public final InpScalar<Long> inp = new InpScalar<>(this);
    int pubIndex = 0;
    public int cnt;

    public ScalarSubscriberActor(Dataflow df, int delay, int count,  PublisherActor... pubs) {
        super(df);
        logger.setLevel(Level.ALL);
        this.delay = delay;
        this.cnt = count;
        this.pubs = pubs;
    }

    @Override
    public void start() {
        super.start();
        nextSubscribe();
    }

    public void nextSubscribe() {
        pubs[pubIndex].out.subscribe(inp);
        pubIndex = (pubIndex+1)%pubs.length;
    }

    @Override
    protected void runAction() throws Throwable {
        Thread.sleep(delay);
        Long in = inp.remove();
        if (cnt > 0) {
            cnt--;
            nextSubscribe();
        } else {
            onComplete();
        }
    }
}
