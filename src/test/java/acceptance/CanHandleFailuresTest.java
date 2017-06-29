package acceptance;

import com.google.zxing.BarcodeFormat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tdl.record.image.input.ImageInput;
import tdl.record.image.input.InputFromErrorProneSource;
import tdl.record.image.input.InputFromFreezingSource;
import tdl.record.image.input.InputFromStreamOfBarcodes;
import tdl.record.image.output.OutputToInMemoryBuffer;
import tdl.record.time.FakeTimeSource;
import tdl.record.time.TimeSource;
import tdl.record.video.VideoPlayer;
import tdl.record.video.VideoRecorder;
import tdl.record.video.VideoRecorderException;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

public class CanHandleFailuresTest {

    private ExecutorService executor;

    @Before
    public void setUp() throws Exception {
        executor = Executors.newSingleThreadExecutor();
    }

    @After
    public void tearDown() throws Exception {
        executor.shutdownNow();
    }

    @Test
    public void recorded_file_should_be_recoverable_after_sigkill() throws Exception {
        String destinationVideo = "build/recording_interrupted_by_sigkill.mp4";
        TimeSource recordTimeSource = new FakeTimeSource();
        ImageInput imageInput = new InputFromStreamOfBarcodes(BarcodeFormat.CODE_39, 300, 150, recordTimeSource);
        VideoRecorder videoRecorder = new VideoRecorder
                .Builder(new InputFromFreezingSource(121, imageInput))
                .withFragmentation(1, TimeUnit.SECONDS)
                .withTimeSource(recordTimeSource).build();

        // Start video in a separate thread. The thread should block on the image source
        videoRecorder.open(destinationVideo, 5, 1);
        executor.submit(() -> {
            try {
                videoRecorder.start(Duration.of(5, ChronoUnit.MINUTES));
            } catch (VideoRecorderException e) {
                e.printStackTrace();
            }
        });

        //Allow the recorder to perform the recording and stop half-way
        Thread.sleep(5000);

        // Read current video file
        VideoPlayer videoPlayer = new VideoPlayer(new OutputToInMemoryBuffer(), recordTimeSource);
        videoPlayer.open(destinationVideo);
        assertThat("Video duration is not as expected", videoPlayer.getDuration(), greaterThan(Duration.of(1, ChronoUnit.SECONDS)));
    }

    @Test
    public void recording_should_stop_gracefully_on_fatal_exception() throws Exception {
        String destinationVideo = "build/recording_interrupted_by_exception.mp4";
        TimeSource recordTimeSource = new FakeTimeSource();
        ImageInput imageInput = new InputFromStreamOfBarcodes(BarcodeFormat.CODE_39, 300, 150, recordTimeSource);
        VideoRecorder videoRecorder = new VideoRecorder
                .Builder(new InputFromErrorProneSource(10, imageInput))
                .withTimeSource(recordTimeSource).build();

        // Capture video
        videoRecorder.open(destinationVideo, 5, 1);
        try {
            videoRecorder.start(Duration.of(60, ChronoUnit.SECONDS));
        } catch (Exception e) {
            System.out.println("Exception caught and ignored: "+e.getMessage());
        }
        videoRecorder.close();

        // Read recorded video parameters
        VideoPlayer videoPlayer = new VideoPlayer(new OutputToInMemoryBuffer(), recordTimeSource);
        videoPlayer.open(destinationVideo);
        assertThat("Video duration is not as expected", videoPlayer.getDuration(), is(Duration.of(2, ChronoUnit.SECONDS)));
    }
}