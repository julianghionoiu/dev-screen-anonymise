package acceptance;

import com.google.zxing.BarcodeFormat;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Ignore;
import org.junit.Test;
import tdl.record.image.input.*;
import tdl.record.image.output.OutputToBarcodeReader;
import tdl.record.image.output.OutputToInMemoryBuffer;
import tdl.record.metrics.RecordingMetricsCollector;
import tdl.record.metrics.RecordingListener;
import tdl.record.time.FakeTimeSource;
import tdl.record.time.SystemTimeSource;
import tdl.record.time.TimeSource;
import tdl.record.utils.ImageQualityHint;
import tdl.record.video.VideoPlayer;
import tdl.record.video.VideoRecorder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static java.lang.Math.abs;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CanRemoveSubimageTest {

    @Test
    public void can_remove_subimage() throws Exception {

        String destinationVideo = "build/recording_from_barcode_at_4x.mp4";
        TimeSource recordTimeSource = new FakeTimeSource();
        ImageInput imageInput = new InputFromStreamOfBarcodes(BarcodeFormat.CODE_39, 300, 150, recordTimeSource);
        VideoRecorder videoRecorder = new VideoRecorder.Builder(imageInput).withTimeSource(recordTimeSource).build();

        // Capture video
        videoRecorder.open(destinationVideo, 5, 4);
        videoRecorder.start(Duration.of(12, ChronoUnit.SECONDS));
        videoRecorder.close();

        // Read recorded video parameters
        TimeSource replayTimeSource = new FakeTimeSource();
        OutputToBarcodeReader barcodeReader = new OutputToBarcodeReader(replayTimeSource, BarcodeFormat.CODE_39);
        VideoPlayer videoPlayer = new VideoPlayer(barcodeReader, replayTimeSource);
        videoPlayer.open(destinationVideo);
        assertThat("Video duration is not as expected", videoPlayer.getDuration(), is(Duration.of(3, ChronoUnit.SECONDS)));
        assertThat(videoPlayer.getFrameRate(), is(closeTo(20, 0.01)));
        assertThat(videoPlayer.getWidth(), is(300));
        assertThat(videoPlayer.getHeight(), is(150));

        // Play the recorded video and read the barcodes
        videoPlayer.play();
        videoPlayer.close();

        // Assert on timestamps
        List<OutputToBarcodeReader.TimestampPair> decodedBarcodes = barcodeReader.getDecodedBarcodes();
        assertThat(decodedBarcodes.isEmpty(), is(false));
        assertThat(decodedBarcodes, areConsistentWith(4));
    }
}
