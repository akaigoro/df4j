package com.github.rfqu.df4j.testutil;

import java.util.concurrent.CountDownLatch;

import com.github.rfqu.df4j.core.Callback;

/**
 * The ending node. This is Port rather than actor, to be accessed outside
 * the actor world.
 */
public class MessageSink<T> extends CountDownLatch implements Callback<T> {

    public MessageSink(int count) {
        super(count);
    }

    @Override
    public void post(T message) {
        super.countDown();
    }

    @Override
    public void postFailure(Throwable exc) {
        super.countDown();
        exc.printStackTrace();
    }

}