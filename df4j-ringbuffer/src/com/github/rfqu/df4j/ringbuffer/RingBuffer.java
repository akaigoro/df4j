package com.github.rfqu.df4j.ringbuffer;

public class RingBuffer<T> {
    protected final int bufSize;
    protected Object[] buffer;
    protected int indexMask;
    /** window for writing  */
    Window writeWindow=new Window();
    /** window for reading  */
    Window readWindow=new Window();

    public RingBuffer(int bufSize) {
        if (bufSize < 1) {
            throw new IllegalArgumentException("bufferSize must not be less than 1");
        }
        if (Integer.bitCount(bufSize) != 1) {
            throw new IllegalArgumentException("bufferSize must be a power of 2");
        }
        this.bufSize = bufSize;
        this.indexMask = bufSize - 1;
        buffer=new Object[bufSize];
        writeWindow.peer=readWindow;
        writeWindow.limit=bufSize;
        readWindow.peer=writeWindow;
    }

    public void connect(BoltNode.Window<T> source, BoltNode.Window<T> sink) {
        writeWindow.setListener(source);
        source.setRbw(writeWindow);
        readWindow.setListener(sink);
        sink.setRbw(readWindow);
    }
    /**
     * ring buffer access 
     */
    public class Window {
        Window peer;
        long position=0;
        long limit;
        BoltNode.Window<T> listener;

        public int getBufSize() {
            return bufSize;
        }

        public void setListener(BoltNode.Window<T> listener) {
            this.listener=listener;
            listener.up(size());
        }

        public void set(int index, T element) {
            buffer[((int)position+index) & indexMask]=element;
        }

        public void post(T element) {
            buffer[((int)position) & indexMask]=element;
            shift(1);
        }

        @SuppressWarnings("unchecked")
        public T get(int index) {
            int i = ((int)position+index) & indexMask;
            T res = (T) buffer[i];
            buffer[i]=null;
            return res;
        }

        @SuppressWarnings("unchecked")
        public T get() {
            int i = ((int)position) & indexMask;
            T res = (T) buffer[i];
            buffer[i]=null;
            return res;
        }

        void shift(int delta) {
            long newPosition = position+delta;
            if (newPosition>limit) {
                throw new IllegalArgumentException("new position="+newPosition+">"+limit);
            }
            position=newPosition;
            peer.shiftLimit(delta);
        }

        void shiftLimit(int delta) {
            limit+=delta;
            listener.up(delta);
        }

        public boolean isEmpty() {
            return position==limit;
        }
        
        public int size() {
            return (int)(limit-position);
        }
    }
}
