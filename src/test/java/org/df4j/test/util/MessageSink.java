package org.df4j.test.util;

import java.util.concurrent.CountDownLatch;
import org.df4j.core.Port;

/**
 * The ending node. This is Port rather than actor, to be accessed outside
 * the actor world.
 */
public class MessageSink<T> extends CountDownLatch implements Port<T> {//Callback<T> {

    public MessageSink(int count) {
        super(count);
    }

    @Override
    public void post(T message) {
        super.countDown();
    }
}