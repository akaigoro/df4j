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

import static com.github.rfqu.df4j.testutil.Utils.byteArraysEqual;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.github.rfqu.df4j.core.DFContext;
import com.github.rfqu.df4j.ext.ImmediateExecutor;
import com.github.rfqu.df4j.pipeline.ByteChunkSink;
import com.github.rfqu.df4j.pipeline.CharChunkSource;

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
        test("Я");
    }
    
    @Test
    public void testCyrillic() throws IOException {
        test("Овсянка, sir");
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
        CharChunkSource source = new CharChunkSource();
        UTF82Byte decoder=new UTF82Byte(source,4);
        ByteChunkSink sink = new ByteChunkSink(decoder);
        
        source.post(s);
        assertFalse(sink.isClosed());
        source.close();
        assertTrue(sink.isClosed());
        assertTrue(byteArraysEqual(string2Bytes(s), sink.getRes()));
    }
}