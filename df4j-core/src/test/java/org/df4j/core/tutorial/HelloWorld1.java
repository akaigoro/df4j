package org.df4j.core.tutorial;

import org.df4j.core.actor.ext.Actor1;
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
        coll.onNext("Hello");
        coll.onNext("World");
        coll.onComplete();
    }


    public void A1() { System.out.println("A1") ; }
    public void A2() { System.out.println("A2") ;}
    public void A3() {System.out.println("A3") ;}
    public void A4() {System.out.println("A4") ;}

    public static interface MoveAction {
        void move();
    }

    private MoveAction[] moveActions = new MoveAction[] {
            new MoveAction() { public void move() {
                System.out.println("A1") ;
            } },
            () -> { System.out.println("A2") ;},
            this::A3,
            this::A4,
    };

    public void move(int index) {
        moveActions[1].move();
    }

    public static void main(String[] a) {
        HelloWorld1 inst = new HelloWorld1();
        inst.move(1);
    }
}
