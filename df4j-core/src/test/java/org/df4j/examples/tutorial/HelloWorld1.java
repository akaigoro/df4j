package org.df4j.examples.tutorial;

import org.df4j.core.node.Actor1;
import org.df4j.test.util.DebugActor;
import org.df4j.test.util.DebugActor1;
import org.junit.Test;

public class HelloWorld1 {
    /**
     * collects strings
     * prints collected strings when argument is an empty string 
     */
     class Collector extends DebugActor1<String> {
         StringBuilder sb=new StringBuilder(); 
         
         @Override
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
         Collector coll=new Collector();
         coll.post("Hello");
         coll.post("World");
         coll.complete();
     }

}
