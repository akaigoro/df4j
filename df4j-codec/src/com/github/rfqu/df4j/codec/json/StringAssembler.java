package com.github.rfqu.df4j.codec.json;

import java.io.IOException;
import java.util.ArrayDeque;

/** converts instructions to String
 * 
 * @author Alexei Kaigorodov
 *
 */
public abstract class StringAssembler implements JsonAssembler {
    final Appendable sink;
    ArrayDeque<String> stack=new ArrayDeque<String>(); 
    boolean commaNeeded=false;
        
    public StringAssembler(Appendable sink) {
        this.sink = sink;
    }

    public void startList() throws IOException {
        sink.append("[");
        stack.push("]");
        commaNeeded=false;
    }
    
    public void startSet() throws IOException {
        sink.append("{");
        stack.push("}");    
        commaNeeded=false;
    }
    
    public void addInt(int value) throws IOException {
        if (commaNeeded) {
            sink.append(',');
        } else {
            commaNeeded=true;
        }
        sink.append(Integer.toString(value));
    }
    
    public void addString(String value) throws IOException {
        if (commaNeeded) {
            sink.append(',');
        } else {
            commaNeeded=true;
        }
        sink.append(value);
    }

    public void setKey(String key) throws IOException {
        sink.append(key);
        sink.append(':');
    }
    
    public void close() throws IOException {
        sink.append(stack.pop());
    }
}
