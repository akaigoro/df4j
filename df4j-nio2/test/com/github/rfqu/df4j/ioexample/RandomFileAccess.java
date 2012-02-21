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

import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;

import com.github.rfqu.df4j.core.*;
import com.github.rfqu.df4j.io.*;

public class RandomFileAccess {

    final static int blockSize = 4096*16; // bytes
    final static long numBlocks = 500; // items
    final static long fileSize = blockSize * numBlocks; // bytes
    final static int maxBufNo = 8; // max number of buffers
    PrintStream out = System.out;
    String testfilename="testfile";
    
    @Before
    public void init() {
        out.println("File of size " + fileSize + " with " + numBlocks + " blocks of size " + blockSize);
//        out.println("has="+ByteBuffer.allocate(blockSize).hasArray());
    }

    public static void main(String args[]) throws Exception {
        RandomFileAccess tst = new RandomFileAccess();
        tst.init();
        tst.testW_IO();
        tst.testW_NIO();
        tst.testW_dffwS();
        tst.testW_dffwJUC();
        // tst.testR();
    }

    /**
     * writes file using traditional java.io facilities
     * @throws Exception
     */
    @Test
    public void testW_IO() throws Exception {
        out.println("testW_IO: java.io");
        try {
            RandomAccessFile rf = new RandomAccessFile(testfilename, "rw");
            rf.setLength(blockSize*numBlocks);
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < numBlocks; i++) {
                long blockId = getBlockId(numBlocks, i);
                ByteBuffer buf = ByteBuffer.allocate(blockSize);
                fillBuf(buf, blockId);
                rf.seek(blockId*blockSize);
                rf.write(buf.array());
            }
            rf.close();
            float etime = System.currentTimeMillis() - startTime;
            out.println("elapsed=" + etime / 1000 + " sec; mean io time=" + (etime / numBlocks) + " ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * writes file using java.nio, indirect buffers, and futures - without DF framework
     *  @throws Exception
     */
    @Test
    public void testW_NIO() throws Exception {
        testW_NIO(false);
    }

    /**
     * writes file using java.nio, direct buffers, and futures - without DF framework
     *  @throws Exception
     */
    @Test
    public void testW_NIOD() throws Exception {
        testW_NIO(true);
    }

    private void testW_NIO(boolean direct) throws IOException, Exception {
        out.println("testW_NIO: NIO with futuires; direct="+direct);
        AsynchronousFileChannel af = AsynchronousFileChannel.open(Paths.get(testfilename), WRITE);
        af.truncate(blockSize*numBlocks);
        for (int nb = 1; nb <= maxBufNo; nb=nb*2) {
            long startTime = System.currentTimeMillis();
            testWnio(af, nb, false);
            float etime = System.currentTimeMillis() - startTime;
            out.println("num bufs=" + nb + " elapsed=" + etime / 1000 + " sec; mean io time=" + (etime / numBlocks) + " ms");
        }
        af.force(true);
        af.close();
    }

    void testWnio(AsynchronousFileChannel af, int nb, boolean direct) throws Exception {
        Requestnio[] reqs = new Requestnio[nb];
        for (int k = 0; k < nb; k++) {
            reqs[k] = new Requestnio(af, direct);
        }
        for (int i = 0; i < numBlocks; i++) {
            long blockId = getBlockId(numBlocks, i);
            Requestnio req = reqs[i % nb];
            req.await();
            fillBuf(req.buf, blockId);
            req.write(blockId);
        }
    }

    /**
     * combines file, ByteBuffer, and Future
     */
    static class Requestnio {
        Future<Integer> fut = null;
        AsynchronousFileChannel af;
        ByteBuffer buf;

        public Requestnio(AsynchronousFileChannel af, boolean direct) {
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
     * writes file using AsynchronousFileChannel, indirect buffers, and DF framework with SimpleExecutorService
     *  @throws Exception
     */
    @Test
    public void testW_dffwS() throws Exception {
        SimpleExecutorService executor = new SimpleExecutorService();
        testW_dffw(executor, false);
    }

    /**
     * writes file using AsynchronousFileChannel, direct buffers, and DF framework with SimpleExecutorService
     * @throws Exception
     */
    @Test
    public void testW_dffwSD() throws Exception {
        SimpleExecutorService executor = new SimpleExecutorService();
        testW_dffw(executor, true);
    }

    /**
     * writes file using AsynchronousFileChannel, indirect buffers, and DF framework with java.util.concurrent.ExecutorService
     * @throws Exception
     */
    @Test
    public void testW_dffwJUC() throws Exception {
        ThreadFactoryTL tf = new ThreadFactoryTL();
        ExecutorService executor = Executors.newFixedThreadPool(2, tf);
        tf.setExecutor(executor);
        testW_dffw(executor, false);
    }

    /**
     * writes file using AsynchronousFileChannel, direct buffers, and DF framework with java.util.concurrent.ExecutorService
     * @throws Exception
     */
    @Test
    public void testW_dffwJUCD() throws Exception {
        ThreadFactoryTL tf = new ThreadFactoryTL();
        ExecutorService executor = Executors.newFixedThreadPool(2, tf);
        tf.setExecutor(executor);
        testW_dffw(executor, true);
    }

    /** general dataflow test
     * 
     * @param executor context executor (accessible via thread context)
     * @param direct if true, use direct buffers
     * @throws Exception
     */
    public void testW_dffw(ExecutorService executor, boolean direct) throws Exception {
        out.println("Using " + executor.getClass().getCanonicalName()+" direct="+direct);
        Task.setCurrentExecutor(executor);
        AsynchronousFileChannel af = AsyncFileChannel.open(Paths.get(testfilename), WRITE);
        af.truncate(blockSize*numBlocks);
        for (int nb = 1; nb <= maxBufNo; nb=nb*2) {
            long startTime = System.currentTimeMillis();
            StarterW command = new StarterW(af, nb, direct);
            executor.execute(command);
            int res = command.sink.get();
            af.force(true);
            float etime = System.currentTimeMillis() - startTime;
            out.println("num bufs=" + nb + " elapsed=" + etime / 1000 + " sec; mean io time=" + (etime / numBlocks) + " ms");
        }
        af.close();
        executor.shutdown();
    }

    /** starting task
     * creates the Writer actor and sends it empty buffers
     *
     */
    class StarterW extends Task {
        AsynchronousFileChannel af;
        int nb;
        boolean direct;
        PortFuture<Integer> sink = new PortFuture<Integer>();
        WriterActor wa = new WriterActor();

        public StarterW(AsynchronousFileChannel af, int nb, boolean direct) {
            this.af = af;
            this.nb = nb;
            this.direct = direct;
        }

        @Override
        public void run() {
            try {
                for (int k = 0; k < nb; k++) {
                    ByteBuffer buf = direct?ByteBuffer.allocateDirect(blockSize):ByteBuffer.allocate(blockSize);
                    FileIORequest req = new FileIORequest(af, buf);
                    wa.send(req);
                }
            } catch (Exception e) {
                sink.send(1);
            }
        }

        class WriterActor extends Actor<FileIORequest> {
            int started = 0; // number of started io operations
            int finished = 0;// number of finished io operations

            @Override
            protected void act(FileIORequest req) throws Exception {
                if (req.getResult() != null || req.getExc() != null) { // is it empty or used buffer?
                    finished++;
                    if (finished == numBlocks) { // has the whole file been written?
                        setReady(false);
                        sink.send(0); // signal the caller
                        return;
                    }
                }
                if (started < numBlocks) { // has all io requests been launched?
                    req.clear();
                    long blockId = getBlockId(numBlocks, started);
                    fillBuf(req.getBuffer(), blockId);
                    req.write(blockId * blockSize, this);
                    started++;
                }
            }

            @Override
            protected void complete() throws Exception {
                // TODO Auto-generated method stub
                
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
        buf.flip();
    }

    /**
     * pseudo-randomizer, to simulate access to random file blocks 
     */
    public static long getBlockId(long range, int i) {
        return (i * 0x5DEECE66DL + 0xBL) % range;
    }
}
