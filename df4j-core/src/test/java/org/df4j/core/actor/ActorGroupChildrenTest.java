package org.df4j.core.actor;

import org.df4j.core.activities.PublisherActor;
import org.df4j.core.activities.RangeActor;
import org.df4j.core.activities.SubscriberActor;
import org.junit.Test;

public class ActorGroupChildrenTest {

    @Test
    public void printChildrenTest() throws InterruptedException {
        MyActorGroup df = new MyActorGroup();
        RangeActor pub = new RangeActor(df, 0, 0);
        SubscriberActor sub = new SubscriberActor(df, 0);
        pub.out.subscribe(sub);
        String ch = df.getChildren();
        System.out.println(ch);
    }

    private class MyActorGroup extends ActorGroup {
        String getChildren() {
            return children.toString();
        }
    }
}

