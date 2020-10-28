package org.df4j.core.actor;

import java.util.concurrent.ExecutorService;

/**
 * Analogue of Petri Net's transition.
 * Ports are like places, but can belong to only one transition.
 * Tokens are tokens :).
 */
public interface Transition {

    AsyncProc getParentActor();

    int registerPort(AsyncProc.Port port);

    void block(AsyncProc.Port port);

    void unblock(AsyncProc.Port port);
}
