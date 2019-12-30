package org.df4j.core.portadapter;

import org.df4j.core.activities.PublisherActor;
import org.df4j.core.activities.SubscriberActor;
import org.df4j.core.dataflow.Dataflow;
import org.junit.Test;

public class FlowReactiveTest {

    @Test
    public void test1() {
        Dataflow parent = new Dataflow();
        int total=10;
        PublisherActor publisher = new PublisherActor(parent, total, 0);
        ReactiveProcessor processor = new ReactiveProcessor(parent,  0);
        SubscriberActor subscriber = new SubscriberActor(parent,  50);
        Reactive2FlowSubscriber<Integer> proxy1 = new Reactive2FlowSubscriber<>(processor.inp);
        publisher.out.subscribe(proxy1);
        Flow2ReactiveSubscriber proxy2 = new Flow2ReactiveSubscriber(subscriber.inp);
        processor.out.subscribe(proxy2);
        publisher.start();
        processor.start();
        subscriber.start();
        parent.blockingAwait(1000);
    }

    public  static void  main(String[] args) {
        new FlowReactiveTest().test1();
        System.out.println("passed");
    }
}
