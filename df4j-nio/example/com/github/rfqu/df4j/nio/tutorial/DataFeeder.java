package com.github.rfqu.df4j.nio.tutorial;

import java.nio.ByteBuffer;

import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.nio.AsyncSocketChannel;

/**
 * Feeds supplied channel with data of specified size.
 * Data are array of integers.
 * First integer is size, rest are 0...size-1.
 */
class DataFeeder extends Actor<MyRequest> {
    AsyncSocketChannel asc;
    int size;
    int counter=0;

    public DataFeeder(AsyncSocketChannel asc, int size) {
        this.asc = asc;
        this.size = size;
    }

    public void write() {
        MyRequest request = new MyRequest(512);
        ByteBuffer buf = request.getBuffer();
        buf.putInt(size);
        post(request);
    }

    /** next Buffer is available for writing */
    @Override
    protected void act(MyRequest request) throws Exception {
        ByteBuffer buf = request.getBuffer();
        int remaining = buf.remaining()/4;
        int limit=size-counter;
        if (limit==0) return; // nothing to do
        if (limit>remaining) {
            limit=remaining;
        }
        for (int k=counter; k<counter+limit; k++) {
            buf.putInt(k);
        }
        counter+=limit;
        if (size==counter) return; // nothing to do
        asc.write(request);
    }
}