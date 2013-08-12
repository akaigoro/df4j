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
import java.io.UnsupportedEncodingException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.rfqu.df4j.codec.chars.UTF82Byte;
import com.github.rfqu.df4j.core.DFContext;
import com.github.rfqu.df4j.ext.ImmediateExecutor;
import com.github.rfqu.df4j.pipeline.ByteSink;
import com.github.rfqu.df4j.pipeline.ByteIterator;
import com.github.rfqu.df4j.pipeline.CharSink;
import com.github.rfqu.df4j.pipeline.CharIterator;

public class UTF82ByteTest {
	@BeforeClass
	public static void init() {
		DFContext.setCurrentExecutor(new ImmediateExecutor());
	}
    
    @Test
    public void testACII1() throws IOException {
        test("a");
    }
    
    @Test
    public void testACII2() throws IOException {
        test("ascii");
    }
    
    @Test
    public void testCyrillic1() throws IOException {
        test("�");
    }
    
    @Test
    public void testCyrillic() throws IOException {
        test("���� ��� ����");
    }

    protected byte[] string2Bytes(String s) throws UnsupportedEncodingException, IOException {
        ByteArrayOutputStream outs=new ByteArrayOutputStream();
        OutputStreamWriter out=new OutputStreamWriter(outs, "UTF8");
        out.write(s);
        out.close();
        byte[] bytes=outs.toByteArray();
        return bytes;
    }

    void test(String s) throws IOException {
        CharEnumerator charSource=new CharEnumerator();
        UTF82Byte decoder=new UTF82Byte(charSource);
        ByteCollector chp=new ByteCollector(decoder);
        
        charSource.postString(s);
        byte[] arr=new byte[10]; 
        chp.read(arr);
        
        String res=chp.toString();
        Assert.assertEquals(s, res);
    }
    
    static class CharEnumerator implements CharIterator {
        CharSink sink;

        void postString(String s) {
            for (int k=0; k<s.length(); k++) {
                sink.postChar(s.charAt(k));
            }

        }
        @Override
        public void demand(CharSink sink) {
            this.sink=sink;
        }
        
    }

    static class ByteCollector implements ByteSink {
        ByteIterator source;
        byte[] data;
        int pos=0;

        public ByteCollector(ByteIterator source) {
            this.source=source;
        }

        @Override
        public boolean postByte(byte b) {
            data[pos++]=b;
            return pos<data.length;
        }

        @Override
        public String toString() {
            try {
                return new String(data, 0, pos, "UTF8");
            } catch (UnsupportedEncodingException e) {
                return e.toString();
            }
        }

        @Override
        public void postEOF() {
            // TODO Auto-generated method stub
            
        }
        
        //==============================
        
        void read(byte data[]) {
            this.data=data;
            pos=0;
            source.demand(this);
        }
        
    }
}