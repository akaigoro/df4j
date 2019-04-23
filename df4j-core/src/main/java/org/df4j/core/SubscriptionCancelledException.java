package org.df4j.core;

/**
 * access to cancelled subscription attempted
 */
public class SubscriptionCancelledException extends Exception {

    public SubscriptionCancelledException() {
    }

    public SubscriptionCancelledException(String message) {
        super(message);
    }
}
