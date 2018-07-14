package org.df4j.core.connector.reactivestream;

import org.df4j.core.connector.messagescalar.SimpleSubscription;

public interface ReactiveSubscription extends SimpleSubscription {

    void request(long n);

}
