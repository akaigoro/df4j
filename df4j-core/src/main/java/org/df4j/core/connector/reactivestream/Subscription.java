package org.df4j.core.connector.reactivestream;

import org.df4j.core.connector.messagescalar.SimpleSubscription;

public interface Subscription extends SimpleSubscription {

    void request(long n);

}
