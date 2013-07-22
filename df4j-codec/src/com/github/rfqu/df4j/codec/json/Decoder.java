package com.github.rfqu.df4j.codec.json;

import java.io.IOException;


public interface Decoder extends Scanner {
    
    public void scanned(int tag, String res) throws IOException;

}
