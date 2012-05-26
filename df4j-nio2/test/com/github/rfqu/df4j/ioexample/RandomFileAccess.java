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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

import com.github.rfqu.df4j.core.*;
import com.github.rfqu.df4j.nio2.*;

public class RandomFileAccess {
    final static int blockSize = 4096*16; // bytes
    final static long numBlocks = 500; // items
    final static long fileSize = blockSize * numBlocks; // bytes
    final static int maxBufNo = 8; // max number of buffers
    PrintStream out = System.out;
    File testfile;
    Path testfilePath;
    
    @Before
    public void init() {
        testfile=new File("testfile.dat");
        testfile.deleteOnExit();
        testfilePath = Paths.get(testfile.getAbsolutePath());
        out.println("File of size " + fileSize + " with " + numBlocks + " blocks of size " + blockSize);
    }

    public static void main(String args[]) throws Exception {
        RandomFileAccess tst = new RandomFileAccess();
        tst.init();
        tst.testW_IO();
        tst.testW_NIO();
        tst.testW_dffwS();
    }

    /**
     * writes file using traditional java.io facilities
     * @throws Exception
     */
    @Test
    public void testW_IO() throws Exception {
        out.println("testW_IO: java.io");
        try {
            RandomAccessFile rf = new RandomAccessFile(testfile, "rw");
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
    //@Test
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
        AsynchronousFileChannel af 
            = new AsyncFileChannel(testfilePath, WRITE).getChannel();
        af.truncate(blockSize*numBlocks);
        for (int nb = maxBufNo*2; nb >0; nb=nb/2) {
            long startTime = System.currentTimeMillis();
            testWnio(af, nb, false);
            float etime = System.currentTimeMillis() - startTime;
            out.println("num bufs=" + nb + " elapsed=" + etime / 1000 + " sec; throughput=" + (etime / numBlocks) + " ms");
        }
        af.close();
    }

    void testWnio(AsynchronousFileChannel af, int nb, boolean direct) throws Exception {
        Requestnio[] reqs = new Requestnio[nb];
        for (int k = 0; k < nb; k++) {
            reqs[k] = new Requestnio(direct);
        }
        for (int i = 0; i < numBlocks; i++) {
            long blockId = getBlockId(numBlocks, i);
            Requestnio req = reqs[i % nb];
            req.await();
            fillBuf(req.buffer, blockSize * blockId);
            req.write(af, blockSize * blockId);
        }
        for (int k = 0; k < nb; k++) {
            reqs[k].await();
        }
        af.force(true);
    }

    /**
     * combines ByteBuffer and Future
     */
    static class Requestnio {
        Future<Integer> fut = null;
        ByteBuffer buffer;

        public Requestnio(boolean direct) {
            buffer = direct?ByteBuffer.allocateDirect(blockSize):ByteBuffer.allocate(blockSize);
        }

        public void await() throws InterruptedException, ExecutionException {
            if (fut==null) {
                return;
            }
            fut.get();
            fut=null;
        }

        public void write(AsynchronousFileChannel af, long pos) {
            buffer.flip();
            fut = af.write(buffer, pos);
        }
    }

    /**
     * writes file using AsynchronousFileChannel and indirect buffers
     *  @throws Exception
     */
    //@Test
    public void testW_dffwS() throws Exception {
        boolean direct=false;
        out.println("testW_dffw: NIO2; direct="+direct);
        AsyncFileChannel af = new AsyncFileChannel(testfilePath, WRITE);
        testW_dffw(af,direct);
    }

    /**
     * writes file using AsynchronousFileChannel and direct buffers
     * @throws Exception
     */
    @Test
    public void testW_dffwSD() throws Exception {
        boolean direct=true;
        out.println("testW_dffwSD: NIO2; direct="+direct);
        AsyncFileChannel af = new AsyncFileChannel(testfilePath, WRITE);
        testW_dffw(af,direct);
    }

    /** general dataflow test
     * 
     * @param executor context executor (accessible via thread context)
     * @param direct if true, use direct buffers
     * @throws Exception
     */
    public void testW_dffw(AsyncFileChannel af, boolean direct) throws Exception {
        af.truncate(blockSize*numBlocks);
        for (int nb = maxBufNo; nb >0; nb=nb/2) {
            long startTime = System.currentTimeMillis();
            StarterW command = new StarterW(af, nb, direct);
            int res = command.sink.get();
            af.force(true);
            float etime = System.currentTimeMillis() - startTime;
            out.println("res="+res+" num bufs=" + nb + " elapsed=" + etime / 1000 + " sec; throughput=" + (etime / numBlocks) + " ms");
            out.println("mean io time="+((float)command.accTime.get())/1000000/numBlocks+" ms");
            if (res!=0) {
                out.println("ERROR:"+res);
            }
        }
        af.close();
    }

    /** starting task
     * creates the Writer actor and sends it empty buffers
     *
     */
    class StarterW {
        AsyncFileChannel af;
        int nb;
        boolean direct;
        long started=0;
        long finished=0;
        AtomicLong accTime=new AtomicLong();
        PortFuture<Integer> sink = new PortFuture<Integer>();

        public StarterW(AsyncFileChannel af, int nb, boolean direct) {
            this.af = af;
            this.nb = nb;
            this.direct = direct;
            try {
                for (int k = 0; k < nb; k++) {
                    ByteBuffer buf = direct?ByteBuffer.allocateDirect(blockSize):ByteBuffer.allocate(blockSize);
                    Request req = new Request(buf);
                    act(req);
                }
            } catch (Exception e) {
                sink.send(1);
            }
        }
        
        protected synchronized void act(Request req) {
            if (started < numBlocks) { // has all io requests been launched?
                req.clear();
                long blockId = getBlockId(numBlocks, started);
                fillBuf(req.getBuffer(), blockId);
                try {
                    af.write(req, blockId * blockSize);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                started++;
            }
        }
        
        class Request extends FileIORequest {
            private long start;

            public Request(ByteBuffer buf) {
                super(buf);
            }

            @Override
            public void startWrite(long position) {
                super.startWrite(position);
                start = System.nanoTime();
            }

            @Override
            public void writeCompleted(Integer result, AsyncFileChannel channel) {
                accTime.addAndGet(System.nanoTime()-start);
                finished++;
                if (finished < numBlocks) { // has the whole file been written?
                    act(this);
                } else {                    
                    sink.send(0); // signal the caller
                }
            }

            @Override
            public void writeFailed(Throwable exc, AsyncFileChannel channel) {
                exc.printStackTrace();
                sink.send(1); // signal the caller
            }
            
        }
    }

    static void fillBuf(ByteBuffer buffer, long blockId) {
        buffer.clear();
        /*
        int capacity8 = buf.capacity()/8;
        long start = blockId*capacity8;
        for (int j = 0; j < capacity8; j++) {
            try {
                buf.putLong(start+j);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return;
            }
        }
        */
        buffer.position(buffer.limit()); // imitate writing
    }

    /**
     * pseudo-randomizer, to simulate access to random file blocks 
     */
    public static long getBlockId(long range, long i) {
        return (i * 0x5DEECE66DL + 0xBL) % range;
    }
}
