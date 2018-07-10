package org.df4j.core.util.invoker;

import java.util.function.BiConsumer;

public class BiConsumerInvoker<U,V> extends AbstractInvoker<BiConsumer<U,V>, Void> {

    public BiConsumerInvoker(BiConsumer<U, V> consumer) {
        super(consumer);
    }

    public Void apply(Object... args) {
        assert args.length == 2;
        function.accept((U) args[0], (V) args[1]);
        return null;
    }

}
