package org.df4j.core.tutorial;

import org.df4j.core.tasknode.messagestream.Actor1;
import org.junit.Test;

public class HelloWorld1 {
    /**
     * collects strings
     * prints collected strings when argument is an empty string
     */
    class Collector extends Actor1<String> {
        StringBuilder sb = new StringBuilder();

        protected void runAction(String message) {
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
