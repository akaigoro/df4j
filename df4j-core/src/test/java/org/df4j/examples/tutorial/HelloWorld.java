package org.df4j.examples.tutorial;

import org.df4j.core.connector.messagestream.StreamInput;
import org.df4j.core.node.Actor;
import org.df4j.core.util.SameThreadExecutor;
import org.df4j.test.util.DebugActor;
import org.junit.Test;

import java.util.concurrent.Executor;

public class HelloWorld {
    /**
     * collects strings
     * prints collected strings when argument is an empty string 
     */
     class Collector extends DebugActor {

        StreamInput<String> input=new StreamInput<String>(this); // actor's parameter
        StringBuilder sb=new StringBuilder();
         
         @Override
         protected void act() {
             String message=input.current();
             // empty string indicates the end of stream
             // nulls are not allowed
             if (message==null) {
                 System.out.println(sb.toString());
                 sb.setLength(0);
             } else {
                sb.append(message);
                sb.append(" ");
             }
         }
     }

     @Test
     public void test() {
         Collector coll=new Collector();
         coll.input.post("Hello");
         coll.input.post("World");
         coll.input.complete();
     }

}
