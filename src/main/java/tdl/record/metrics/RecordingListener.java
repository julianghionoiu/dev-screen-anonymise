package tdl.record.metrics;

import io.humble.video.Rational;

import java.util.concurrent.TimeUnit;

public interface RecordingListener {

    void notifyRecordingStart(String destinationFilename, Rational inputFrameRate, Rational videoFrameRate);

    void notifyFrameRenderingStart(long timestamp, TimeUnit unit, long frameIndex);

    void notifyFrameRenderingEnd(long timestamp, TimeUnit unit, long frameIndex);

    void notifyRecordingEnd();
}
