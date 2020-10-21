package org.df4j.core.actor;

import java.util.concurrent.ExecutorService;

public interface Transition {

    ActorGroup getDataflow();

    int registerPort(AsyncProc.Port port);

    ExecutorService getExecutor();

    void unblock(AsyncProc.Port port);

    void block(AsyncProc.Port port);
}
