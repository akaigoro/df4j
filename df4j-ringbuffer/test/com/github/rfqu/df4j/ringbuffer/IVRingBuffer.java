package com.github.rfqu.df4j.ringbuffer;

import com.github.rfqu.df4j.ringbuffer.RingBuffer;
import com.github.rfqu.df4j.testutil.IntValue;

public class IVRingBuffer extends RingBuffer<IntValue> {

    public IVRingBuffer(int bufSize) {
        super(bufSize);
    }

    public IVRingBuffer() {
        super(1024);
    }

    public void fill() {
        for (int k=0; k<bufSize; k++) {
            buffer[k]=new IntValue(-1);
        }
    }

}
