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

public class CanRecordVideoTest {

    @Test
    @Ignore("Manual acceptance")
    public void record_screen_at_4x() throws Exception {
        VideoRecorder videoRecorder = new VideoRecorder
                .Builder(new ScaleToOptimalSizeImage(ImageQualityHint.HIGH, new InputFromScreen()))
                .build();

        videoRecorder.open("text.mp4", 4, 4);
        videoRecorder.start(Duration.of(30, ChronoUnit.SECONDS));

        videoRecorder.close();
    }

    @Test
    @Ignore("Manual acceptance")
    public void multiple_screens() throws Exception {
        //TODO Try the recording when you have multiple screens
    }

    @Test
    public void can_record_video_at_specified_frame_rate_and_speed() throws Exception {
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

    @Test
    public void size_should_be_kept_small_while_retaining_quality() throws Exception {
        String referenceVideoFile = "src/test/resources/t_reference_recording.mp4";
        String destinationVideoFile = "build/recording_from_reference_video.mp4";
        TimeSource timeSource = new FakeTimeSource();
        ImageInput imageInput = new ScaleToOptimalSizeImage(ImageQualityHint.MEDIUM,
                new InputFromVideoFile(referenceVideoFile, timeSource));

        VideoRecorder videoRecorder = new VideoRecorder.Builder(imageInput).withTimeSource(timeSource).build();
        videoRecorder.open(destinationVideoFile, 4, 4);
        videoRecorder.start(Duration.of(60, ChronoUnit.SECONDS));
        videoRecorder.close();

        // Lock down the destination size
        long sizeOfReference = Files.size(Paths.get(referenceVideoFile));
        long sizeOfDestination = Files.size(Paths.get(destinationVideoFile));
        double compressionFactor = (double) sizeOfReference / (double) sizeOfDestination;
        System.out.println("compressionFactor = " + compressionFactor);
        // snap 1/sec = 2.79 comp
        // snap 2/sec = 2.43 comp
        // snap 3/sec = 2.22 comp
        //>snap 4/sec = 2.08 comp
        // snap 5/sec = 1.97 comp
        // snap 6/sec = 1.91 comp
        assertThat(compressionFactor, greaterThan(2.08d));
    }


    @Test
    public void large_screen_size_should_be_scaled_down() throws Exception {
        String referenceImage = "src/test/resources/4k_wallpaper.jpg";
        String destinationVideoFile = "build/recording_from_static_image.mp4";

        ImageInput imageInput = new ScaleToOptimalSizeImage(ImageQualityHint.LOW,
                new InputFromStaticImage(referenceImage));
        RecordingListener metrics = new RecordingMetricsCollector();

        VideoRecorder videoRecorder = new VideoRecorder.Builder(imageInput)
                .withTimeSource(new SystemTimeSource())
                .withRecordingListener(metrics)
                .build();
        videoRecorder.open(destinationVideoFile, 10, 4);
        videoRecorder.start(Duration.of(1, ChronoUnit.SECONDS));
        videoRecorder.close();

        VideoPlayer videoPlayer = new VideoPlayer(new OutputToInMemoryBuffer());
        videoPlayer.open(destinationVideoFile);
        assertThat(videoPlayer.getWidth(), is(1280));
        assertThat(videoPlayer.getHeight(), is(720));
        videoPlayer.close();
    }


    /**
     * Frame rate sampling. On large desktops, taking a screenshot could take a lot of time.
     */
    @Test
    public void test_measure_recording_performance() throws Exception {
        //Record first video
        int lowRateSnapsPerSecond = 2;
        RecordingMetricsCollector metricsForLowFramerate = new RecordingMetricsCollector();
        {
            String destinationVideoFile = "build/recording_for_metrics1.mp4";
            ImageInput imageInput = new ScaleToOptimalSizeImage(ImageQualityHint.LOW, new InputFromScreen());
            VideoRecorder videoRecorder = new VideoRecorder.Builder(imageInput)
                    .withRecordingListener(metricsForLowFramerate).build();
            videoRecorder.open(destinationVideoFile, lowRateSnapsPerSecond, 4);
            videoRecorder.start(Duration.of(1, ChronoUnit.SECONDS));
            videoRecorder.close();
        }

        //Record second video
        int highRateSnapsPerSecond = 4;
        RecordingMetricsCollector metricsForHighFramerate = new RecordingMetricsCollector();
        {
            String destinationVideoFile = "build/recording_for_metrics2.mp4";
            ImageInput imageInput = new ScaleToOptimalSizeImage(ImageQualityHint.LOW, new InputFromScreen());
            VideoRecorder videoRecorder = new VideoRecorder.Builder(imageInput)
                    .withRecordingListener(metricsForHighFramerate).build();
            videoRecorder.open(destinationVideoFile, highRateSnapsPerSecond, 4);
            videoRecorder.start(Duration.of(1, ChronoUnit.SECONDS));
            videoRecorder.close();
        }

        //Assert on performance metrics
        double maxSnapsPerSecond1 = lowRateSnapsPerSecond / metricsForLowFramerate.getRenderingTimeRatio();
        System.out.println("snapsPerSecond:"+lowRateSnapsPerSecond+
                ", renderingTimeRatio = "+metricsForLowFramerate.getRenderingTimeRatio()+
                ", maxSnapsPerSecond = "+maxSnapsPerSecond1);
        double maxSnapsPerSecond2 = highRateSnapsPerSecond / metricsForHighFramerate.getRenderingTimeRatio();
        System.out.println("snapsPerSecond:"+highRateSnapsPerSecond+
                ", renderingTimeRatio = "+metricsForHighFramerate.getRenderingTimeRatio()+
                ", maxSnapsPerSecond = "+maxSnapsPerSecond2);
        assertThat(maxSnapsPerSecond1, closeTo(maxSnapsPerSecond2, 1.5));
    }

    @Test
    public void lock_file_should_be_created_before_starting_recording_and_deleted_after_ending() throws Exception {
        String destinationVideo = "build/recording_from_barcode_at_4x.mp4";
        TimeSource recordTimeSource = new FakeTimeSource();
        ImageInput imageInput = new InputFromStreamOfBarcodes(BarcodeFormat.CODE_39, 300, 150, recordTimeSource);
        VideoRecorder videoRecorder = new VideoRecorder.Builder(imageInput).withTimeSource(recordTimeSource).build();

        Path videoFilePath = Paths.get(destinationVideo);
        Path lockFilePath = Paths.get(destinationVideo + ".lock");
        //delete files if exist before start
        Files.deleteIfExists(lockFilePath);
        Files.deleteIfExists(videoFilePath);
        assertFalse(Files.exists(lockFilePath));
        assertFalse(Files.exists(videoFilePath));

        // Capture video
        videoRecorder.open(destinationVideo, 5, 4);
        //check that lock file created
        assertTrue(Files.exists(lockFilePath));
        //assert that lock file is older than video file
        BasicFileAttributes lockFileAttributes = Files.readAttributes(lockFilePath, BasicFileAttributes.class);
        BasicFileAttributes videoFileAttributes = Files.readAttributes(videoFilePath, BasicFileAttributes.class);
        Instant lockFileCreatingTime = lockFileAttributes.creationTime().toInstant();
        Instant videoFileCreatingTime = videoFileAttributes.creationTime().toInstant();
        assertTrue(lockFileCreatingTime.isBefore(videoFileCreatingTime) || lockFileCreatingTime.equals(videoFileCreatingTime));

        //write video
        videoRecorder.start(Duration.of(12, ChronoUnit.SECONDS));
        videoRecorder.close();
        //check that lock file deleted
        assertFalse(Files.exists(lockFilePath));
    }

    /**
     * Packets produced: The video should be broken down in discrete packets to make upload easier. (write every X min)
     */
    @Test
    @Ignore("Not implemented")
    public void video_should_be_broken_down_into_discrete_packets_to_help_with_upload() throws Exception {
        //TODO Before doing this, play a spike. What is the best way to upload a file?
        //TODO One big file + Multipart, or multiple small files.

        //Check generated files and assert frames contain the right timestamp
    }


    /**
     * UX: feedback should be collected and displayed on the console ( recording status, size generated, growth rate, size available )
     */
    @Test
    @Ignore("Not implemented")
    public void display_processing_feedback_to_the_console() throws Exception {

    }

    /**
     * Add 4x watermark
     */
    @Test
    @Ignore("Not implemented")
    public void the_frames_should_contain_a_watermark_with_the_speedup() throws Exception {

    }

    //~~~~~~~~~~~~~ Helpers

    private Matcher<List<OutputToBarcodeReader.TimestampPair>> areConsistentWith(
            @SuppressWarnings("SameParameterValue") int timeSpeedUpFactor) {
        return new TypeSafeMatcher<List<OutputToBarcodeReader.TimestampPair>>() {
            OutputToBarcodeReader.TimestampPair firstError;

            @Override
            protected boolean matchesSafely(List<OutputToBarcodeReader.TimestampPair> timestamps) {
                Predicate<OutputToBarcodeReader.TimestampPair> timestampNotConsistentWithSpeed = timestampPair -> {
                    double maximumDrift = 1000*1000;
                    return abs((timestampPair.systemTimestamp * timeSpeedUpFactor) - timestampPair.barcodeTimestamp) > maximumDrift;
                };
                Optional<OutputToBarcodeReader.TimestampPair> first = timestamps.stream()
                        .filter(timestampNotConsistentWithSpeed)
                        .findFirst();

                if (first.isPresent()) {
                    firstError = first.get();
                    return false;
                } else {
                    return true;
                }
            }

            @Override
            protected void describeMismatchSafely(List<OutputToBarcodeReader.TimestampPair> timestamps,
                                                  Description mismatchDescription) {
                mismatchDescription.appendText("timestamps have drifted").appendText("\n")
                        .appendText("For system timestamp ").appendValue(firstError.systemTimestamp)
                        .appendText(" expected barcode timestamp ").appendValue(firstError.systemTimestamp * timeSpeedUpFactor)
                        .appendText(" got ").appendValue(firstError.barcodeTimestamp);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("that timestamps are consistent with speedup factor " + timeSpeedUpFactor);
            }
        };
    }

}