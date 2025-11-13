package org.isaiahp;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramLogWriter;
import org.HdrHistogram.SingleWriterRecorder;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class HistoLogger extends Thread {
    private final SingleWriterRecorder recorder;
    private final long intervalMillis;
    private volatile boolean stop;
    private Histogram intervalHisto = null;
    private final Histogram accumulatedHistogram = new Histogram(TimeUnit.SECONDS.toNanos(4), 3);
    private final HistogramLogWriter histogramLogWriter;

    public HistoLogger(Path p, SingleWriterRecorder histogram, TimeUnit seconds, int i) throws FileNotFoundException {
        this.recorder = histogram;
        intervalMillis = seconds.toMillis(i);
        histogramLogWriter = new HistogramLogWriter(p.toFile());
        this.setName("HISTO_RECORDER");
        this.setDaemon(true);
    }

    @Override
    public void run() {
        long lastRecordTime = System.currentTimeMillis();
        IO.println("starting histoLogger");
        while (!stop) {
            final long nowMillis = System.currentTimeMillis();
            final long elapsedTime = nowMillis - lastRecordTime;
            if (elapsedTime > intervalMillis) {
                intervalHisto = recorder.getIntervalHistogram(intervalHisto);
                if (intervalHisto.getTotalCount() > 0) {
                    histogramLogWriter.outputIntervalHistogram(intervalHisto);
                    accumulatedHistogram.add(intervalHisto);
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        IO.println("stopped histoLogger");
    }
    public Histogram getAccumulatedHistogram() {
        return accumulatedHistogram;
    }

    public void stopRecording() {
        this.stop = true;
        histogramLogWriter.close();
    }
}
