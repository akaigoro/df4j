package org.df4j.core.boundconnector.permitscalar;

/**
 *  inlet for permits.
 */
public interface ScalarPermitSubscriber {

    void release();
    default void postFailure(Throwable ex) {}

}
