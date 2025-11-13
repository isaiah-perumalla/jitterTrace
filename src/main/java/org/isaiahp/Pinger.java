package org.isaiahp;


import org.HdrHistogram.*;
import org.agrona.BitUtil;
import org.agrona.IoUtil;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.ringbuffer.OneToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.agrona.concurrent.ringbuffer.RingBufferDescriptor;
import org.isaiahp.utils.CpuUtils;

import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;


public class Pinger  {

    public static final int MAX_PINGS = 100_000_000;
    public static final String DEV_SHM_PINGER_DAT = "/dev/shm/pinger.dat";
    final OneToOneRingBuffer pingBuffer;
    private final RingBuffer pongBuffer;
    private final SingleWriterRecorder histogram = new SingleWriterRecorder(1_000_000_000L, 3);
    private long sendValue = 0;
    private long receivedValue = 0;

    public Pinger(OneToOneRingBuffer pingBuffer, RingBuffer pongBuffer) {
        this.pingBuffer = pingBuffer;

        this.pongBuffer = pongBuffer;
    }

    public int run() {
        int count = 0;
        do {
            final int index = pingBuffer.tryClaim(1, 2*Long.BYTES);
            if (index > 0) {
                pingBuffer.buffer().putLong(index, sendValue++);
                pingBuffer.buffer().putLong(index + Long.BYTES, System.nanoTime());
                pingBuffer.commit(index);
            }
            else {
                break;// shouldnt happend
            }
            while(pongBuffer.read(this::readPong, 1) == 0) {
                //wait for reply
            }
            count++;
            if (count == 10000) {
                histogram.reset(); // warm up
            }

        }while (count < MAX_PINGS);
        return count;
    }

    private void readPong(int msgType, MutableDirectBuffer mutableDirectBuffer, int offset, int length) {

        assert length == 8;
        final long pongValue = mutableDirectBuffer.getLong(offset);
        assert pongValue == receivedValue : "unexpected echoed value ";
        final long ts = mutableDirectBuffer.getLong(offset+Long.BYTES);
        histogram.recordValue(System.nanoTime() - ts);
        receivedValue++;
    }

    static void main() throws Throwable {
        OneToOneRingBuffer pingerMappedBuffer = createNewMappedBuffer(DEV_SHM_PINGER_DAT, 16 * 1000_000L);
        Path pongerPath = Paths.get(PingListener.DEV_SHM_PINGER_ECHO_DAT);
        IO.println(String.format("Pinger tid=%d, any key to start", CpuUtils.getNativeThreadId()));
        System.in.read();
        while(!Files.exists(pongerPath)) { //wait for ponger
            Thread.sleep(1000);
            IO.println("awaiting " + pongerPath);
        }
        Pinger pinger = new Pinger(pingerMappedBuffer, mapExistingFileToRing(pongerPath));
        final Path logPath = Paths.get("./", "memoryPingPong."+
                Long.toHexString(System.currentTimeMillis()) + ".hlog");
        final HistoLogger histoLogger = new HistoLogger(logPath, pinger.latencyHist(), TimeUnit.MILLISECONDS, 500);

        IO.println("starting pinger ");
        histoLogger.start();
        long count = pinger.run();
        IO.println("pinger count " + count);
        histoLogger.stopRecording();
        IO.println("awaiting " + histoLogger.getName());
        histoLogger.join();
        logHistogram(histoLogger.getAccumulatedHistogram());
    }


    private static void logHistogram(Histogram histogram) {
        IO.println(String.format("P50:%d ns", histogram.getValueAtPercentile(0.5)));
        IO.println(String.format("P90:%d ns", histogram.getValueAtPercentile(0.9)));
        IO.println(String.format("P99:%d ns", histogram.getValueAtPercentile(0.99)));
        IO.println(String.format("P999:%d ns", histogram.getValueAtPercentile(0.999)));
        IO.println(String.format("P9999:%d ns", histogram.getValueAtPercentile(0.9999)));
        IO.println(String.format("Min:%d, Max:%d, Mean:%f, stdDev:%f ns",
                histogram.getMinNonZeroValue(),
                histogram.getMaxValue(),
                histogram.getMean(),
                histogram.getStdDeviation()));

        for (HistogramIterationValue bucketValue : histogram.logarithmicBucketValues(10, 2)) {
            long countAtThisStep = bucketValue.getCountAddedInThisIterationStep();
            if (countAtThisStep > 0) {
                IO.println(String.format("[%d, %d] -> %d", bucketValue.getValueIteratedFrom(),
                        bucketValue.getValueIteratedTo(), countAtThisStep));
            }
        }
    }

    private SingleWriterRecorder latencyHist() {
        return histogram;
    }

    public static OneToOneRingBuffer createNewMappedBuffer(String filename, long length) throws IOException {
        Path p = Paths.get(filename);
        Files.deleteIfExists(p);
        long size = BitUtil.findNextPositivePowerOfTwo(length) + RingBufferDescriptor.TRAILER_LENGTH;
        File file = p.toFile();
        file.deleteOnExit();
        MappedByteBuffer buffer = IoUtil.mapNewFile(file, size, true);
        AtomicBuffer atomicBuffer = new UnsafeBuffer();
        atomicBuffer.wrap(buffer);
        return new OneToOneRingBuffer(atomicBuffer);
    }

    public static OneToOneRingBuffer mapExistingFileToRing(Path path) {
        MappedByteBuffer pingerMappedBuffer = IoUtil.mapExistingFile(path.toFile(), "ponger");
        assert (BitUtil.isPowerOfTwo(pingerMappedBuffer.capacity() - RingBufferDescriptor.TRAILER_LENGTH));
        AtomicBuffer pinger = new UnsafeBuffer();
        pinger.wrap(pingerMappedBuffer);
        return new OneToOneRingBuffer(pinger);
    }
}
