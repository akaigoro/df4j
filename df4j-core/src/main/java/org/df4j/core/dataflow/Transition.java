package org.df4j.core.dataflow;

import java.util.concurrent.ExecutorService;

public interface Transition {

    Dataflow getDataflow();

    int registerPort(AsyncProc.Port port);

    ExecutorService getExecutor();

    void unblock(AsyncProc.Port port);

    void block(AsyncProc.Port port);
}
