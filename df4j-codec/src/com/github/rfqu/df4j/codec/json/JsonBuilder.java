package com.github.rfqu.df4j.codec.json;

import java.io.IOException;

/**
 * LIST: startList, VALUE*, end
 * SET: startSet, (setKey, VALUE)*, end
 * VALUE: addInt|addString|LIST|SET
 * 
 * @author Alexei Kaigorodov
 *
 */
public interface JsonBuilder {
    public void startList() throws IOException;
    public void startSet() throws IOException;
    public void addInt(int value) throws IOException;
    public void addString(String value) throws IOException;
    public void setKey(String key) throws IOException;
    public void end() throws IOException;
}
