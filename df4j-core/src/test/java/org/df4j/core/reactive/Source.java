package org.df4j.core.reactive;

import org.df4j.core.Actor;

/**
 * emits totalNumber of Integers and closes the stream
 */
class Source extends Actor {
    OneShotPublisher<Integer> pub = new OneShotPublisher<>(this);
    int totalNumber;
    int val = 0;
    public int sent = 0;

    public Source(int totalNumber) {
        this.totalNumber = totalNumber;
    }

    @Override
    protected void act() {
        ReactorTest.println("pub.post("+val+")");
        checkClose();
        pub.post(val);
        sent++;
        val++;
        checkClose();
    }

    private void checkClose() {
        if (val >= totalNumber) {
            pub.close();
            ReactorTest.println("pub.close()");
        }
    }

}
