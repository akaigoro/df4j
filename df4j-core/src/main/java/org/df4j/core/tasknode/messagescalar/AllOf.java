package org.df4j.core.tasknode.messagescalar;

import org.df4j.core.boundconnector.messagescalar.ScalarPublisher;
import org.df4j.core.boundconnector.messagescalar.ScalarSubscriber;

public class AllOf extends AsyncSupplier<Void> {

    public AllOf() {
    }

    public AllOf(ScalarPublisher<?>... sources) {
        for (ScalarPublisher source: sources) {
            add(source);
        }
    }

    public synchronized void add(ScalarPublisher source) {
        source.subscribe(new Enter());
    }

    @Override
    protected void fire() {
        completeResult(null);
    }

    class Enter extends Lock implements ScalarSubscriber<Object> {

        @Override
        public boolean complete(Object value) {
            return super.turnOn();
        }

        @Override
        public boolean completeExceptionally(Throwable ex) {
            synchronized (AllOf.this) {
                if (!result.isDone()) {
                    AllOf.this.completeResultExceptionally(ex);
                }
            }
            return true;
        }
    }

}
