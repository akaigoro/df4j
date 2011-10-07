/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.ioexample;

import static java.nio.file.StandardOpenOption.WRITE;

import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import com.github.rfqu.df4j.core.*;
import com.github.rfqu.df4j.io.*;
import com.github.rfqu.df4j.util.MessageSink;

public class RandomFileAccess {
    final static int blockSize = 4096*16; // bytes
    final static int numBlocks = 2000; // blocks
    final static long fileSize = blockSize * numBlocks; // byres
    final static int maxBufNo = 8; // max number of buffers
    final static boolean direct=true;
    PrintStream out = System.out;
    String testfilename="testfile";
    
    @Before
    public void init() {
        out.println("File of size " + fileSize + " with " + numBlocks + " blocks of size " + blockSize+" direct="+direct);
//        out.println("has="+ByteBuffer.allocate(blockSize).hasArray());
    }

    /**
     * writes file using traditional java.io facilities
     * @throws Exception
     */
    @Test
    public void testW_IO() throws Exception {
        out.println("Test IO");
        RandomAccessFile rf = new RandomAccessFile(testfilename, "rw");
        rf.setLength(blockSize*numBlocks);
        ByteBuffer buf = ByteBuffer.allocate(blockSize);
        byte[] array = buf.array();
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < numBlocks; i++) {
            fillBuf(buf, i);
            long blockId = getBlockId(i);
            rf.seek(blockId*blockSize);
            rf.write(array);
        }
        rf.close();
        float etime = System.currentTimeMillis() - startTime;
        out.println("elapsed=" + etime / 1000 + " sec; mean io time=" + (etime / numBlocks) + " ms");
    }

    /**
     * writes file using java.nio and futures - without DF framework
     *  @throws Exception
     */
    @Test
    public void testW_NIO() throws Exception {
        out.println("Test NIO with futuries");
        float sum=0;
        for (int nbufs = 1; nbufs <= maxBufNo; nbufs=nbufs*2) {
            float etime = testWnio(nbufs);
            out.println("num bufs=" + nbufs + " elapsed=" + etime / 1000 + " sec; mean io time=" + (etime / numBlocks) + " ms");
            sum+=etime;
        }
        out.println("sum elapsed=" +sum);        
    }

    float testWnio(int nbufs) throws Exception {
        AsynchronousFileChannel af = AsynchronousFileChannel.open(Paths.get(testfilename), WRITE);
        af.truncate(blockSize*numBlocks);
        Requestnio[] reqs = new Requestnio[nbufs];
        for (int k = 0; k < nbufs; k++) {
            reqs[k] = new Requestnio(af);
        }
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < numBlocks; i++) {
            long blockId = getBlockId(i);
            Requestnio req = reqs[i % nbufs];
            req.await();
            fillBuf(req.buf, blockId);
            req.write(blockId);
        }
        af.close();
        return System.currentTimeMillis() - startTime;
    }

    /**
     * combines file, ByteBuffer, and Future
     */
    static class Requestnio {
        Future<Integer> fut = null;
        AsynchronousFileChannel af;
        ByteBuffer buf;

        public Requestnio(AsynchronousFileChannel af) {
            this.af = af;
            buf = direct?ByteBuffer.allocateDirect(blockSize):ByteBuffer.allocate(blockSize);
        }

        public void await() throws InterruptedException, ExecutionException {
            if (fut == null) {
                return;
            }
            fut.get();
            fut = null;
        }

        public void write(long blockId) {
            fut = af.write(buf, blockSize * blockId);
        }
    }

    /**
     * writes file using AsynchronousFileChannel and DF framework with SimpleExecutorService
     *  @throws Exception
     */
    @Test
    public void testW_dffwS() throws Exception {
        SimpleExecutorService executor = new SimpleExecutorService();
        testW_dffw(executor);
    }

    /**
     * writes file using AsynchronousFileChannel, and DF framework with java.util.concurrent.ExecutorService
     * @throws Exception
     */
    @Test
    public void testW_dffwJUC() throws Exception {
        ThreadFactoryTL tf = new ThreadFactoryTL();
        ExecutorService executor = Executors.newFixedThreadPool(2, tf);
        tf.setExecutor(executor);
        testW_dffw(executor);
    }

    /** general dataflow test
     * 
     * @param executor context executor (accessible via thread context)
     * @throws Exception
     */
    public void testW_dffw(ExecutorService executor) throws Exception {
        out.println("Test NIO with DFFW");
        out.println("Using " + executor.getClass().getCanonicalName());
        Task.setCurrentExecutor(executor);
        float sum=0;
        for (int nbufs = 1; nbufs <= maxBufNo; nbufs=nbufs*2) {
            long startTime = System.currentTimeMillis();
            AsynchronousFileChannel af = AsyncFileChannel.open(Paths.get(testfilename), WRITE);
            af.truncate(blockSize*numBlocks);
            StarterW command = new StarterW(af);
            command.start(nbufs);
            command.sink.await();
            af.force(true);
            af.close();
            float etime = System.currentTimeMillis() - startTime;
            out.println("num bufs=" + nbufs + " elapsed=" + etime / 1000 + " sec; mean io time=" + (etime / numBlocks) + " ms");
            sum+=etime;
        }
        out.println("sum elapsed=" +sum);        
        executor.shutdown();
    }

    class StarterW extends AsyncFileChannel {
        AtomicInteger started=new AtomicInteger(0); // counts started requests
        MessageSink sink = new MessageSink(numBlocks); // counts completed requests

        public StarterW(AsynchronousFileChannel af) {
            super(af);
        }

        public void start(int nbufs) {
            for (int k = 0; k < nbufs; k++) {
                FileIORequest req = new FileIORequest(blockSize, direct);
                startRequest(req);
            }
        }

        protected void startRequest(FileIORequest request) {
            long blockId = getBlockId(started.incrementAndGet());
            fillBuf(request.getBuffer(), blockId);
            write(request, blockId);
        }

        @Override
        protected void requestCompleted(FileIORequest request) {
            sink.send(null);
            if (sink.getCount()>0) {
                startRequest(request);
            }
        }
    }

    static void fillBuf(ByteBuffer buf, long blockId) {
        buf.clear();
        int capacity8 = buf.capacity()/8;
        long start = blockId*capacity8;
        for (int j = 0; j < capacity8; j++) {
            buf.putLong(start+j);
        }
    }

    /**
     * pseudo-randomizer, to simulate access to random file blocks 
     */
    public static long getBlockId(int i) {
        return (i * 0x5DEECE66DL + 0xBL) % numBlocks;
    }

    public static void main(String args[]) throws Exception {
        RandomFileAccess tst = new RandomFileAccess();
        tst.init();
        tst.testW_IO();
        tst.testW_NIO();
        tst.testW_dffwS();
        tst.testW_dffwJUC();
    }

}
