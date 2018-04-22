package org.df4j.core.impl.messagescalar;

import org.df4j.core.spi.messagescalar.Port;
import org.df4j.core.spi.messagescalar.Promise;

public class CompletedPromise<T> implements Promise<T> {
    private final T value;

    public CompletedPromise(T value) {
        this.value = value;
    }

    @Override
    public void postTo(Port<T> request) {
        request.post(value);
    }
}
