package org.df4j.core.boundconnector.reactivestream;

import org.df4j.core.boundconnector.SimpleSubscription;

public interface ReactiveSubscription extends SimpleSubscription {

    void request(long n);

}
