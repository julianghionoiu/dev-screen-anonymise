package tdl.record.metrics;

import io.humble.video.Rational;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RecordingMetricsCollector implements RecordingListener {
    private long expectedTimeBetweenFramesNanos;
    private long timestampBeforeProcessingNanos;
    private double renderingTimeRatio;
    private long totalFrames;
    private Path destinationPath;
    private Rational inputFrameRate;
    private Rational videoFrameRate;
    private boolean isCurrentlyRecording;


    public RecordingMetricsCollector() {
        expectedTimeBetweenFramesNanos = Long.MAX_VALUE;
        inputFrameRate = Rational.make();
        videoFrameRate = Rational.make();
        renderingTimeRatio = 0;
        timestampBeforeProcessingNanos = 0;
        totalFrames = 0;
    }

    //~~~~~~~~~~ Collectors

    @Override
    public void notifyRecordingStart(String destinationFilename, Rational inputFrameRate, Rational videoFrameRate) {
        this.destinationPath = Paths.get(destinationFilename);
        this.inputFrameRate = inputFrameRate;
        this.videoFrameRate = videoFrameRate;
        double timeBetweenFramesMillis = inputFrameRate.getValue() * 1000;
        this.expectedTimeBetweenFramesNanos = TimeUnit.MILLISECONDS.toNanos((long) timeBetweenFramesMillis);
        this.isCurrentlyRecording = true;
        log.info("Start recording to \"" + destinationPath.getFileName() + "\"" +
                " at " + videoFrameRate.getDenominator() + " fps" + " " +
                "with " + inputFrameRate.getDenominator() + " screenshots/sec");
    }

    @Override
    public void notifyFrameRenderingStart(long timestamp, TimeUnit unit, long frameIndex) {
        log.debug("Snap ! {}", frameIndex);
        timestampBeforeProcessingNanos = unit.toNanos(timestamp);
    }

    @Override
    public void notifyFrameRenderingEnd(long timestamp, TimeUnit unit, long frameIndex) {
        long timeSpendProcessingNanos = unit.toNanos(timestamp) - timestampBeforeProcessingNanos;
        renderingTimeRatio = timeSpendProcessingNanos / (double) expectedTimeBetweenFramesNanos;
        log.debug("renderingTimeRatio: {}", renderingTimeRatio);
        totalFrames = frameIndex;
    }

    @Override
    public void notifyRecordingEnd() {
        this.isCurrentlyRecording = false;
        log.info("Recording stopped");
    }


    //~~~~~~~~~~ Getters

    public boolean isCurrentlyRecording() {
        return isCurrentlyRecording;
    }

    public Path getDestinationPath() {
        return destinationPath;
    }

    public Rational getInputFrameRate() {
        return inputFrameRate;
    }

    public Rational getVideoFrameRate() {
        return videoFrameRate;
    }

    /**
     * @return the percentage of time spent processing vs the percentage of time spent for a frame
     */
    public double getRenderingTimeRatio() {
        return renderingTimeRatio;
    }

    /**
     * @return total number of processed frames
     */
    public long getTotalFrames() {
        return totalFrames;
    }
}
