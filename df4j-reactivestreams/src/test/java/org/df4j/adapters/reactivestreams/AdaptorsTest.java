package org.df4j.adapters.reactivestreams;

import org.df4j.core.dataflow.Dataflow;
import org.testng.annotations.Test;

public class AdaptorsTest {

    @Test
    public void test1() {
        Dataflow parent = new Dataflow();
        long total=10;
        ReactivePublisher publisher = new ReactivePublisher(parent, total);
        publisher.start();
        ReactiveSubscriber subscriber = new ReactiveSubscriber(parent, total);
        subscriber.start();
        publisher.subscribe(subscriber);
        parent.blockingAwait(100000);
    }

    public  static void  main(String[] args) {
        new AdaptorsTest().test1();
        System.out.println("passed");
    }
}
