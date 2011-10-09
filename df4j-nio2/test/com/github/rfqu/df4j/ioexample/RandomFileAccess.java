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

import static java.nio.file.StandardOpenOption.*;

import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import com.github.rfqu.df4j.core.*;
import com.github.rfqu.df4j.io.*;

public class RandomFileAccess {
    final static int blockSize = 4096*4; // bytes
    final static int numBlocks = 2000; // blocks
    final static long fileSize = blockSize * numBlocks; // byres
    final static int maxBufNo = 16; // max number of buffers
    final static boolean direct=true;
    final static boolean metaData = false;
    final static PrintStream out = System.out;
    final static String testfilename="testfile2";
    
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
        FileChannel channel = rf.getChannel();
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
        channel.force(metaData);
        rf.close();
        float etime = System.currentTimeMillis() - startTime;
        out.println("elapsed=" + etime / 1000 + " sec; mean io time=" + (etime / numBlocks) + " ms");
    }

    /**
     * writes file using synchronous java.nio facilities
     * @throws Exception
     */
    @Test
    public void testW_NIO() throws Exception {
        out.println("Test NIO");
        FileChannel channel = FileChannel.open(Paths.get(testfilename), CREATE, WRITE);
        channel.truncate(blockSize*numBlocks);
        ByteBuffer buf = ByteBuffer.allocate(blockSize);
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < numBlocks; i++) {
            long blockId = getBlockId(i);
            fillBuf(buf, blockId);
            buf.flip();
            channel.write(buf, blockId*blockSize);
            buf.flip();
        }
        channel.force(metaData);
        channel.close();
        float etime = System.currentTimeMillis() - startTime;
        out.println("elapsed=" + etime / 1000 + " sec; mean io time=" + (etime / numBlocks) + " ms");
    }

    /**
     * writes file using java.nio and futures - without DF framework
     *  @throws Exception
     */
    @Test
    public void testW_NIO2F() throws Exception {
        out.println("Test NIO2 with futuries");
        float sum=0;
        for (int nbufs = 1; nbufs <= maxBufNo; nbufs=nbufs*2) {
            float etime = testWnio2F(nbufs);
            out.println("num bufs=" + nbufs + " elapsed=" + etime / 1000 + " sec; mean io time=" + (etime / numBlocks) + " ms");
            sum+=etime;
        }
        out.println("sum elapsed=" +sum);        
    }

    float testWnio2F(int nbufs) throws Exception {
        AsynchronousFileChannel channel = AsynchronousFileChannel.open(Paths.get(testfilename), CREATE, WRITE);
        channel.truncate(blockSize*numBlocks);
        ArrayBlockingQueue<Requestnio> reqs = new ArrayBlockingQueue<Requestnio>(nbufs);
        for (int k = 0; k < nbufs; k++) {
            reqs.add(new Requestnio());
        }
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < numBlocks; i++) {
            long blockId = getBlockId(i);
            Requestnio req = reqs.take();
            Integer res=req.await();
            if (res!=null) {
//                out.println("req.written:"+res);
            }
            fillBuf(req.buf, blockId);
            long filepos = blockId*blockSize;
//            out.println("req.write:"+filepos);
            req.write(channel, filepos);
            reqs.add(req);
        }
        for (int k = 0; k < nbufs; k++) {
            reqs.take().await();
        }
        channel.force(metaData);
        channel.close();
        return System.currentTimeMillis() - startTime;
    }

    /**
     * combines file, ByteBuffer, and Future
     */
    static class Requestnio {
        Future<Integer> fut = null;
        ByteBuffer buf;

        public Requestnio() {
            buf = direct?ByteBuffer.allocateDirect(blockSize):ByteBuffer.allocate(blockSize);
        }

        public Integer await() throws InterruptedException, ExecutionException {
            if (fut == null) {
                return null;
            }
            Integer res = fut.get();
            fut = null;
            buf.flip();
            return res;
        }

        public void write(AsynchronousFileChannel af, long filepos) {
            buf.flip();
            fut = af.write(buf, filepos);
        }
    }

    /**
     * writes file using java.nio and completion handlers - without DF framework
     *  @throws Exception
     */
    @Test
    public void testW_NIO2C() throws Exception {
        out.println("Test NIO2 with completions");
        float sum=0;
        for (int nbufs = 1; nbufs <= maxBufNo; nbufs=nbufs*2) {
            float etime = testWnio2C(nbufs);
            out.println("num bufs=" + nbufs + " elapsed=" + etime / 1000 + " sec; mean io time=" + (etime / numBlocks) + " ms");
            sum+=etime;
        }
        out.println("sum elapsed=" +sum);        
    }

    float testWnio2C(int nbufs) throws Exception {
        AsynchronousFileChannel channel = AsynchronousFileChannel.open(Paths.get(testfilename), CREATE, WRITE);
        channel.truncate(blockSize*numBlocks);
        ArrayBlockingQueue<RequestnioC> reqs = new ArrayBlockingQueue<RequestnioC>(nbufs);
        for (int k = 0; k < nbufs; k++) {
            reqs.add(new RequestnioC());
        }
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < numBlocks; i++) {
            long blockId = getBlockId(i);
            RequestnioC req = reqs.take();
            if (req.result!=null) {
//                out.println("req.written:"+req.result);
            }
            fillBuf(req.buf, blockId);
            long filepos = blockId*blockSize;
//            out.println("req.write:"+filepos);
            req.write(channel, filepos, reqs);
        }
        for (int k = 0; k < nbufs; k++) {
            reqs.take();
        }
        channel.force(metaData);
        channel.close();
        return System.currentTimeMillis() - startTime;
    }

    /**
     * combines file, ByteBuffer, and CompletionHandler
     */
    static class RequestnioC implements CompletionHandler<Integer, ArrayBlockingQueue<RequestnioC>>{
        ByteBuffer buf;
        long filepos;
        boolean intrans=false;
        private Integer result;

        public RequestnioC() {
            buf = direct?ByteBuffer.allocateDirect(blockSize):ByteBuffer.allocate(blockSize);
        }

        public void write(AsynchronousFileChannel af, long filepos, ArrayBlockingQueue<RequestnioC> reqs) {
            this.filepos=filepos;
//            out.println("RequestnioC.write:=" +filepos);        
            intrans=true;
            result=null;
            buf.flip();
            af.write(buf, filepos, reqs, this);
        }

        @Override
        public void completed(Integer result, ArrayBlockingQueue<RequestnioC> reqs) {
            buf.flip();
            intrans=false;
//            out.println("RequestnioC.completed: " +result);        
            this.result=result;
            reqs.add(this);
        }

        @Override
        public void failed(Throwable exc, ArrayBlockingQueue<RequestnioC> reqs) {
            buf.flip();
            intrans=false;
//            out.println("RequestnioC.failed: " +exc+" filepos="+filepos); 
          //  exc.printStackTrace();
            this.result=new Integer(-2);
            notify();
            reqs.add(this);
        }
    }

    /**
     * writes file using AsynchronousFileChannel and DF framework with default executor
     *  @throws Exception
     */
    @Test
    public void testW_dffwD() throws Exception {
        testW_dffw(null);
    }

    /**
     * writes file using AsynchronousFileChannel and DF framework with SimpleExecutorService
     *  @throws Exception
     */
    @Test
    public void testW_dffwS() throws Exception {
        SimpleExecutorService executor = new SimpleExecutorService();
        testW_dffw(executor);
        executor.shutdown();
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
        executor.shutdown();
    }

    /** general dataflow test
     * 
     * @param executor context executor (accessible via thread context)
     * @throws Exception
     */
    public void testW_dffw(ExecutorService executor) throws Exception {
        out.println("Test NIO with DFFW");
        out.println("Using " + (executor==null?"default executor":executor.getClass().getCanonicalName()));
        Task.setCurrentExecutor(executor);
        float sum=0;
        for (int nbufs = 1; nbufs <= maxBufNo; nbufs=nbufs*2) {
            Writer command = new Writer(Paths.get(testfilename), CREATE, WRITE);
            float etime = command.runtest(nbufs);
            out.println("num bufs=" + nbufs + " elapsed=" + etime / 1000 + " sec; mean io time=" + (etime / numBlocks) + " ms");
            sum+=etime;
        }
        out.println("sum elapsed=" +sum);        
    }

    class Writer extends AsyncFileChannel {
        final AtomicInteger started=new AtomicInteger(0); // counts started requests
        final AtomicInteger finished=new AtomicInteger(0); // counts completed requests
        final CountDownLatch sink = new CountDownLatch(1); 

        public Writer(Path path, StandardOpenOption... options) throws IOException {
            super(path, options);
            channel.truncate(blockSize*numBlocks);
        }

        public float runtest(int nbufs) throws IOException, InterruptedException {
            long startTime = System.currentTimeMillis();
            for (int k = 0; k < nbufs; k++) {
                FileIORequest req = new FileIORequest(blockSize, direct);
                startRequest(req);
            }
            sink.await();
            channel.force(metaData);
            channel.close();
            return System.currentTimeMillis() - startTime;
        }

        protected void startRequest(FileIORequest request) {
            long blockId = getBlockId(started.incrementAndGet());
            long filepos=blockId*blockSize;
//            out.println("startRequest:"+filepos+":"+request.getBuffer().limit());
            fillBuf(request.getBuffer(), blockId);
            write(request, filepos);
        }

        @Override
        protected void requestCompleted(FileIORequest request) {
//            out.println("requestCompleted:"+request.getPosition()+":"+request.getResult());
            if (finished.incrementAndGet()<numBlocks) {
                startRequest(request);
            } else {
                sink.countDown();
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
        tst.testW_NIO2F();
        tst.testW_NIO2C();
        tst.testW_dffwS();
        tst.testW_dffwJUC();
    }

}
