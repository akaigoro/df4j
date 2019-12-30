package org.df4j.core.port;

import org.df4j.core.activities.ObserverActor;
import org.df4j.core.activities.PublisherActor;
import org.df4j.core.dataflow.Dataflow;
import org.junit.Test;

/*
 * how to commect Single.Observer to Flow.Publisher
 */
public class FlowToSingleTest {
    @Test
    public void test1() {
        Dataflow parent = new Dataflow();
        int total=5;
        PublisherActor publisher = new PublisherActor(parent, total, 0);
        ObserverActor observer = new ObserverActor(parent, publisher.out,  50);
        publisher.start();
        observer.start();
        parent.blockingAwait(1000);
    }

    public  static void  main(String[] args) {
        new FlowToSingleTest().test1();
        System.out.println("passed");
    }
}
