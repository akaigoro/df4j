package org.df4j.core.util;

public class CharBuffer {
    public final int bufferCapacity;
    private final char[] charBuffer;
    private int posWrite = 0;
    private int count = 0;

    public CharBuffer(int bufferCapacity) {
        this.bufferCapacity = bufferCapacity;
        charBuffer = new char[bufferCapacity];
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public long remainingCapacity() {
        long res = bufferCapacity - count;
        return res;
    }

    public void add(char ch) {
        charBuffer[(posWrite++)%bufferCapacity] = ch;
        count++;
    }

    public char remove() {
        char res = charBuffer[(posWrite - count) % bufferCapacity];
        count--;
        return res;
    }

    public char current() {
        return charBuffer[(posWrite-1) % bufferCapacity];
    }

    public boolean buffIsFull() {
        return count == bufferCapacity;
    }
}
