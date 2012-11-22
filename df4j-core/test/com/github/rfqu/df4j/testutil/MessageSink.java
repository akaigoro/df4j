package com.github.rfqu.df4j.testutil;

import java.util.concurrent.CountDownLatch;

import com.github.rfqu.df4j.core.Port;

/**
 * The ending node. This is Port rather than actor, to be accessed outside
 * the actor world.
 */
public class MessageSink<T> extends CountDownLatch implements Port<T> {

    public MessageSink(int count) {
        super(count);
    }

    @Override
    public void send(T message) {
        super.countDown();
    }

}