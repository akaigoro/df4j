package org.df4j.core.dataflow;

/**
 * applicable to {@link AsyncProc} also.
 */
public enum ActorState {
    /**
     * created but not yet started
     */
    Created,

    /**
     *  started but some ports are blocked
     */
    Blocked,

    /**
     * started and all port are ready
     */
    Running,

    /**
     * suspended by call to {@link Actor#delay(long)} or  {@link Actor#suspend()}
     */
    Suspended,

    /**
     * completed normally or exceptionally.
     * Will never run again.
     */
    Completed,
}