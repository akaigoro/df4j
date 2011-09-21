package com.github.rfqu.dffw.ioexample;

import static java.nio.file.StandardOpenOption.DELETE_ON_CLOSE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;

import com.github.rfqu.dffw.core.*;
import com.github.rfqu.dffw.io.AsyncFile;
import com.github.rfqu.dffw.io.IORequest;

public class RandomFileAccess {

    final static int blockSize = 4096;
    final static long numBlocks = 10000;
    final static long fileSize = blockSize * numBlocks;
    PrintStream out = System.out;
    Path p = Paths.get("testfile");

    @Before
    public void init() {
        out.println("File of size " + fileSize + " with " + numBlocks + " blocks of size " + blockSize);
        if (!p.exists()) {
            try {
                p.createFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String args[]) throws InterruptedException {
        RandomFileAccess tst = new RandomFileAccess();
        tst.init();
        // tst.testWnio();
        tst.testW();
        // tst.testR();
    }

    @Test
    public void testWnio() throws InterruptedException {
        for (int k = 1; k < 5; k++) {
            testWnio(k);
        }
    }

    void testWnio(int nb) {
        Path p = Paths.get("testfile");
        Requestnio[] reqs = new Requestnio[nb];
        try {
            AsynchronousFileChannel af = AsynchronousFileChannel.open(p, TRUNCATE_EXISTING, WRITE, DELETE_ON_CLOSE);
            for (int k = 0; k < nb; k++) {
                reqs[k] = new Requestnio(af);
            }
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < numBlocks; i++) {
                long blockId = getBlockId(numBlocks, i);
                Requestnio req = reqs[i % nb];
                req.await();
                req.write(blockId);
            }
            float etime = System.currentTimeMillis() - startTime;
            out.println("num bufs=" + nb + " elapsed=" + etime / 1000 + " sec; mean io time=" + (etime / numBlocks) + " ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static long getBlockId(long range, int i) {
        return (i * 0x5DEECE66DL + 0xBL) % range;
    }

    static class Requestnio {
        Future<Integer> fut = null;
        AsynchronousFileChannel af;
        ByteBuffer buf = ByteBuffer.allocateDirect(blockSize);

        public Requestnio(AsynchronousFileChannel af) {
            this.af = af;
        }

        public void await() throws InterruptedException, ExecutionException {
            if (fut == null) {
                return;
            }
            fut.get();
            fut = null;
        }

        public void write(long blockId) {
            {
                fut = af.write(buf, blockSize * blockId);
            }
        }
    }

    @Test
    public void testW() throws InterruptedException {
        String workerName = SimpleExecutorService.class.getCanonicalName();
        out.println("Using " + workerName);
        SimpleExecutorService executor = new SimpleExecutorService();
        Task.setCurrentExecutor(executor);
        for (int k = 1; k < 5; k++) {
            long startTime = System.currentTimeMillis();
            StarterW command = new StarterW(k);
            executor.execute(command);
            int res = command.sink.get();
            float etime = System.currentTimeMillis() - startTime;
            out.println("res=" + res + " elapsed=" + etime / 1000 + " sec; mean io time=" + (etime / numBlocks) + " ms");
        }
        executor.shutdown();
    }

    class StarterW extends Task {
        int nb;
        Promise<Integer> sink = new Promise<Integer>();

        public StarterW(int nb) {
            this.nb = nb;
        }

        @Override
        public void run() {
            Path p = Paths.get("testfile");
            try {
                AsynchronousFileChannel af = AsyncFile.open(p, TRUNCATE_EXISTING, WRITE, DELETE_ON_CLOSE);
                WriterActor wa = new WriterActor();
                for (int k = 0; k < nb; k++) {
                    ByteBuffer buf = ByteBuffer.allocateDirect(blockSize);
                    IORequest req = new IORequest(af, buf);
                    wa.send(req);
                }
            } catch (Exception e) {
                sink.send(1);
            }
        }

        class WriterActor extends Actor<IORequest> {
            int started = 0;
            int finished = 0;

            @Override
            protected void act(IORequest req) throws Exception {
                if (started < numBlocks) {
                    req.clear();
                    long blockId = getBlockId(numBlocks, started);
                    req.write(blockId * blockSize, this);
                    started++;
                }
                if (req.getResult() != null || req.getExc() != null) {
                    finished++;
                    if (finished == numBlocks) {
                        setReady(false);
                        sink.send(0);
                        return;
                    }
                }
            }

        }

    }


}
