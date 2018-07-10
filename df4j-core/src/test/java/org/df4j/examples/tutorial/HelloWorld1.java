package org.df4j.examples.tutorial;

import org.df4j.core.node.Action;
import org.df4j.core.node.messagestream.Actor1;
import org.junit.Test;

public class HelloWorld1 {
    /**
     * collects strings
     * prints collected strings when argument is an empty string
     */
    class Collector extends Actor1<String> {
        StringBuilder sb = new StringBuilder();

        @Action
        protected void act(String message) {
            sb.append(message);
            sb.append(" ");
        }

        protected void onCompleted() {
            System.out.println(sb.toString());
            sb.setLength(0);
        }

    }

    @Test
    public void test() {
        Collector coll = new Collector();
        coll.start();
        coll.post("Hello");
        coll.post("World");
        coll.complete();
    }

}
