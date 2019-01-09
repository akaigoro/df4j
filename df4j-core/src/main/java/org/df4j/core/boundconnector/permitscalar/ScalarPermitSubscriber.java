package org.df4j.core.boundconnector.permitscalar;

/**
 *  inlet for permits.
 */
@FunctionalInterface
public interface ScalarPermitSubscriber {

    void release();
    default void postFailure(Throwable ex) {}

}
