/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.codec.chars;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.junit.Assert;
import org.junit.Test;

import com.github.rfqu.df4j.codec.CharPort;

public class Byte2UTF8Test {

    void test(String s) throws IOException {
        ByteArrayOutputStream outs=new ByteArrayOutputStream();
        OutputStreamWriter out=new OutputStreamWriter(outs, "UTF8");
        out.write(s);
        out.close();
        byte[] bytes=outs.toByteArray();
        CharPort chp=new CharCollector();
        Byte2UTF8 decoder=new Byte2UTF8(chp);
        for (int k=0; k<bytes.length; k++) {
            decoder.postByte(bytes[k]);
        }
        String res=chp.toString();
        Assert.assertEquals(s, res);
    }
    
    @Test
    public void testACII() throws IOException {
        test("ascii");
    }
    
    @Test
    public void testCyrillic1() throws IOException {
        test("А");
    }
    
    @Test
    public void testCyrillic() throws IOException {
        test("Маша ела кашу");
    }
    
    static class CharCollector implements CharPort{
        StringBuilder sb=new StringBuilder();

        @Override
        public void postChar(char b) {
            sb.append(b);
        }

        @Override
        public String toString() {
            return sb.toString();
        }
        
    }
}