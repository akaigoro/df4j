package org.df4j.core.port;

import org.df4j.core.dataflow.AsyncProc;
import org.df4j.core.dataflow.Transitionable;

import java.util.concurrent.ExecutorService;

public class MultiPort extends AsyncProc.Port implements AsyncProc.Transition {

    public MultiPort(Transitionable parent) {
        super(parent);
    }

    @Override
    public int registerPort(AsyncProc.Port port) {
        return parent.registerPort(port);
    }

    @Override
    public ExecutorService getExecutor() {
        return parent.getExecutor();
    }

    @Override
    public void unblock(AsyncProc.Port port) {
        parent.unblock(port);
    }

    @Override
    public void block(AsyncProc.Port port) {

    }

    @Override
    public void setBlocked(int portNum) {

    }
}
