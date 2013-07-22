package com.github.rfqu.df4j.nio.tutorial;

import java.nio.ByteBuffer;

import com.github.rfqu.df4j.nio.SocketIORequest;

public class MyRequest extends SocketIORequest<MyRequest> {
    public MyRequest() {
        this(1024);
    }
    public MyRequest(int size) {
        super(ByteBuffer.allocate(size));
    }
}