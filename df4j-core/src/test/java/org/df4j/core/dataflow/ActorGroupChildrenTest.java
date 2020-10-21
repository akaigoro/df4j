package org.df4j.core.dataflow;

import org.df4j.core.activities.PublisherActor;
import org.df4j.core.activities.SubscriberActor;
import org.df4j.core.actor.ActorGroup;
import org.junit.Test;

public class ActorGroupChildrenTest {

    @Test
    public void printChildrenTest() throws InterruptedException {
        MyActorGroup df = new MyActorGroup();
        PublisherActor pub = new PublisherActor(df, 0, 0);
        SubscriberActor sub = new SubscriberActor(df, 0);
        String ch = df.getChildren();
        System.out.println(ch);
    }

    private class MyActorGroup extends ActorGroup {
        String getChildren() {
            return children.toString();
        }
    }
}

