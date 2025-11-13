package org.isaiahp;

import org.agrona.BitUtil;
import org.agrona.IoUtil;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.ringbuffer.OneToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RingBufferDescriptor;
import org.isaiahp.utils.CpuUtils;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.isaiahp.Pinger.*;

public class PingListener {
    public static final String DEV_SHM_PINGER_ECHO_DAT = "/dev/shm/pinger-echo.dat";
    private final OneToOneRingBuffer echoWriteBuffer;
    private final OneToOneRingBuffer pingReadBuffer;

    public PingListener(OneToOneRingBuffer echoWriteBuffer, OneToOneRingBuffer pingReadBuffer) {
        this.echoWriteBuffer = echoWriteBuffer;
        this.pingReadBuffer = pingReadBuffer;
    }

    public void run() {
        int count = 0;
        do {
            while(pingReadBuffer.read(this::readPing, 1) == 0) {
                //wait for reply
            }
            count++;
        }while (count < MAX_PINGS);
    }

    private void readPing(int msgType, MutableDirectBuffer mutableDirectBuffer, int offset, int length) {
        final int index = echoWriteBuffer.tryClaim(1, 2*Long.BYTES);
        long value = mutableDirectBuffer.getLong(offset);
        long ts = mutableDirectBuffer.getLong(offset + Long.BYTES);
        if (index > 0) {
            echoWriteBuffer.buffer().putLong(index, value);
            echoWriteBuffer.buffer().putLong(index + Long.BYTES, ts);
            echoWriteBuffer.commit(index);
        }
        else {
            throw new IllegalStateException();
        }

    }

    static void main() throws Throwable {
        OneToOneRingBuffer echoRing = createNewMappedBuffer(DEV_SHM_PINGER_ECHO_DAT, 16 * 1000_000L);
        Path pingerPath = Paths.get(Pinger.DEV_SHM_PINGER_DAT);
        IO.println(String.format("PingListener tid=%d, any key to start", CpuUtils.getNativeThreadId()));
        System.in.read();

        while (!Files.exists(pingerPath)) { //wait for ponger
            IO.println("awaiting " + pingerPath);
            Thread.sleep(1000);
        }

        PingListener echo = new PingListener(echoRing, mapExistingFileToRing(pingerPath));
        IO.println("starting Ping Listener ");
        echo.run();
        
    }
}
