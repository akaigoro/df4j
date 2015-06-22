package org.df4j.pipeline;

import org.df4j.core.Actor;
import org.df4j.core.BackPort;
import org.df4j.core.StreamPort;

public abstract class ReactiveActor extends Actor {

    // =============================== Bidirecional Pins (backpressure support)

    public abstract class BaseReactiveStreamOutput<T> implements StreamPort<T>, BackPort<T> {
        protected ReactiveStreamInput<T> inputRef;

        public void connect(ReactiveStreamInput<T> inputRef) {
            this.inputRef = inputRef;
            inputRef.setPackPort(this);
        }

        @Override
        public void post(T message) {
            inputRef.post(message);
        }

        @Override
        public void close() {
            inputRef.close();
        }

        @Override
        public boolean isClosed() {
            return inputRef.isClosed();
        }

        @Override
        public abstract void takeBack(T token);

        @Override
        public abstract void request(int count);

    }

    public class ReactiveStreamOutput<T> extends BaseReactiveStreamOutput<T> implements StreamPort<T>, BackPort<T> {
        StreamInput<T> backPort = new StreamInput<>();

        public void takeBack(T token) {
            backPort.post(token);
        }

        @Override
        public void request(int count) {
            throw new UnsupportedOperationException();
        }

        public T get() {
            return backPort.get();
        }

    }

    public class ReactiveSemStreamOutput<T> extends BaseReactiveStreamOutput<T> implements StreamPort<T>, BackPort<T> {
        Semafor counter = new Semafor();

        public void takeBack(T token) {
            counter.up();
        }

        @Override
        public void request(int count) {
            counter.up(count);
        }

    }

    public class ReactiveStreamInput<T> extends StreamInput<T> {
        BaseReactiveStreamOutput<T> backPort;

        void setPackPort(BaseReactiveStreamOutput<T> backPort) {
            this.backPort = backPort;
        }

        public void takeBack(T token) {
            backPort.takeBack(token);
        }
    }

}
