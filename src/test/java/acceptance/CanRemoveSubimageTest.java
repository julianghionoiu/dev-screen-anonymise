package acceptance;

import com.google.zxing.BarcodeFormat;
import java.awt.image.BufferedImage;
import java.io.File;
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
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import tdl.anonymize.video.VideoMasker;
import tdl.record.video.VideoRecorderException;

public class CanRemoveSubimageTest {

    private static String destinationVideo = "build/recording_from_barcode_at_4x.mp4";

    private static TimeSource recordTimeSource = new FakeTimeSource();
    
    private static ImageInput imageInput = new InputFromStreamOfBarcodes(BarcodeFormat.CODE_39, 300, 150, recordTimeSource);

    @Before
    public void generate_video() throws VideoRecorderException {
        //Creating video
        if (!Files.exists(Paths.get(destinationVideo))) {
            VideoRecorder videoRecorder = new VideoRecorder.Builder(imageInput).withTimeSource(recordTimeSource).build();
            // Capture video
            videoRecorder.open(destinationVideo, 5, 4);
            videoRecorder.start(Duration.of(12, ChronoUnit.SECONDS));
            videoRecorder.close();
        }
    }

    @Test
    public void can_remove_subimage() throws Exception {
        String destinationImage = "build/barcode_subimage.png";
        recordTimeSource.wakeUpAt(1, TimeUnit.SECONDS);
        BufferedImage bufferedImage = imageInput.readImage();
        File outputFile = new File(destinationImage);
        ImageIO.write(bufferedImage, "png", outputFile);

        String maskedDestination = "build/recording_from_barcode_at_4x.masked.mp4";

        VideoMasker masker = new VideoMasker(
                Paths.get(destinationVideo),
                Paths.get(maskedDestination),
                Arrays.asList(new Path[]{Paths.get(destinationImage)})
        );
        masker.run();
        int[] affectedFrames = new int[] {};
        //TODO: Read video frame and see if the affected frames matches
    }
    
    @Test
    public void can_remove_multiple_in_one_frame() throws Exception {
        String maskedDestination = "build/recording_from_barcode_at_4x.masked.1.mp4";

        Path subImage1 = Paths.get("src/test/resources/barcode-subimage-1.png");
        Path subImage2 = Paths.get("src/test/resources/barcode-subimage-2.png");
        VideoMasker masker = new VideoMasker(
                Paths.get(destinationVideo),
                Paths.get(maskedDestination),
                Arrays.asList(new Path[]{subImage1, subImage2})
        );
        masker.run();
        int[] affectedFrames = new int[] {};
        //TODO: Read video frame and see if the affected frames matches
    }
    
    @Test
    public void can_remove_multiple_subimages_in_different_frame() throws Exception {
        String maskedDestination = "build/recording_from_barcode_at_4x.masked.2.mp4";

        Path subImage1 = Paths.get("src/test/resources/barcode-subimage-1.png");
        Path subImage2 = Paths.get("src/test/resources/barcode-subimage-3.png");
        VideoMasker masker = new VideoMasker(
                Paths.get(destinationVideo),
                Paths.get(maskedDestination),
                Arrays.asList(new Path[]{subImage1, subImage2})
        );
        masker.run();
        int[] affectedFrames = new int[] {};
        //TODO: Read video frame and see if the affected frames matches
    }
}
