package org.df4j.core.dataflow;

import org.df4j.core.activities.PublisherActor;
import org.df4j.core.activities.SubscriberActor;
import org.junit.Assert;
import org.junit.Test;

public class DataflowChildrenTest {

    @Test
    public void printChildrenTest() throws InterruptedException {
        MyDataflow df = new MyDataflow();
        PublisherActor pub = new PublisherActor(df, 0, 0);
        SubscriberActor sub = new SubscriberActor(df, 0);
        String ch = df.getChildren();
        System.out.println(ch);
    }

    private class MyDataflow extends Dataflow {
        String getChildren() {
            return children.toString();
        }
    }
}

