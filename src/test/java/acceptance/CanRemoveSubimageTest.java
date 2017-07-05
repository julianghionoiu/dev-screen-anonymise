package acceptance;

import com.google.zxing.BarcodeFormat;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import org.bytedeco.javacv.FrameGrabber;
import org.junit.Before;
import tdl.anonymize.video.VideoMasker;
import tdl.record.video.VideoPlayerException;
import tdl.record.video.VideoRecorderException;

public class CanRemoveSubimageTest {

    private static String barcodeVideo = "build/recording_from_barcode_at_4x.mp4";
    private static String qrCodeVideo = "build/recording_from_qrcode_at_4x.mp4";

    private static TimeSource recordTimeSource = new FakeTimeSource();

    private static TimeSource replayTimeSource = new FakeTimeSource();

    private static ImageInput barcodeInput = new InputFromStreamOfBarcodes(BarcodeFormat.CODE_39, 300, 150, recordTimeSource);

    private static ImageInput qrCodeInput = new InputFromStreamOfBarcodes(BarcodeFormat.QR_CODE, 300, 150, recordTimeSource);

    private static void recordVideo(String path, ImageInput input) throws VideoRecorderException {
        VideoRecorder videoRecorder = new VideoRecorder.Builder(input).withTimeSource(recordTimeSource).build();
        // Capture video
        videoRecorder.open(path, 5, 4);
        videoRecorder.start(Duration.of(12, ChronoUnit.SECONDS));
        videoRecorder.close();
    }

    @Before
    public void generate_video() throws VideoRecorderException, VideoPlayerException, InterruptedException, IOException {
        //Creating video
        if (!Files.exists(Paths.get(barcodeVideo))) {
            recordVideo(barcodeVideo, barcodeInput);
        }

        if (!Files.exists(Paths.get(qrCodeVideo))) {
            recordVideo(qrCodeVideo, qrCodeInput);
        }
    }

    private static List<Long> getReadedBarcodeFromVideo(String path, BarcodeFormat format) throws VideoPlayerException, InterruptedException, IOException {
        OutputToBarcodeReader barcodeReader = new OutputToBarcodeReader(replayTimeSource, format);
        VideoPlayer videoPlayer = new VideoPlayer(barcodeReader, replayTimeSource);
        videoPlayer.open(path);

        videoPlayer.play();
        videoPlayer.close();
        List<OutputToBarcodeReader.TimestampPair> decodedBarcodes = barcodeReader.getDecodedBarcodes();
        return decodedBarcodes.stream().map((pair) -> {
            return pair.barcodeTimestamp;
        }).collect(Collectors.toList());
    }

    @Test
    public void can_remove_subimage() throws Exception {
        String destinationImage = "build/barcode_subimage.png";
        recordTimeSource.wakeUpAt(1, TimeUnit.SECONDS);
        BufferedImage bufferedImage = barcodeInput.readImage();
        File outputFile = new File(destinationImage);
        ImageIO.write(bufferedImage, "png", outputFile);

        String maskedDestination = "build/recording_from_barcode_at_4x.masked.mp4";

        VideoMasker masker = new VideoMasker(
                Paths.get(barcodeVideo),
                Paths.get(maskedDestination),
                Arrays.asList(new Path[]{Paths.get(destinationImage)})
        );
        masker.run();
    }

    @Test
    public void can_remove_multiple_in_one_frame() throws Exception {
        String maskedDestination = "build/recording_from_barcode_at_4x.masked.1.mp4";

        Path subImage1 = Paths.get("src/test/resources/barcode-subimage-1.png");
        Path subImage2 = Paths.get("src/test/resources/barcode-subimage-2.png");
        VideoMasker masker = new VideoMasker(
                Paths.get(barcodeVideo),
                Paths.get(maskedDestination),
                Arrays.asList(new Path[]{subImage1, subImage2})
        );
        masker.run();
        getReadedBarcodeFromVideo(maskedDestination, BarcodeFormat.CODE_39).stream().forEach(System.out::println);
    }

    @Test
    public void can_remove_multiple_subimages_in_different_frame() throws Exception {
        String maskedDestination = "build/recording_from_barcode_at_4x.masked.2.mp4";

        Path subImage1 = Paths.get("src/test/resources/barcode-subimage-1.png");
        Path subImage2 = Paths.get("src/test/resources/barcode-subimage-3.png");
        VideoMasker masker = new VideoMasker(
                Paths.get(barcodeVideo),
                Paths.get(maskedDestination),
                Arrays.asList(new Path[]{subImage1, subImage2})
        );
        masker.run();
        getReadedBarcodeFromVideo(maskedDestination, BarcodeFormat.CODE_39).stream().forEach(System.out::println);
    }

    @Test
    public void can_remove_multiple_subimage_of_a_frame() throws Exception {
        String maskedDestination = "build/recording_from_barcode_at_4x.masked.3.mp4";

        Path subImage1 = Paths.get("src/test/resources/barcode-subimage-4.png");
        VideoMasker masker = new VideoMasker(
                Paths.get(barcodeVideo),
                Paths.get(maskedDestination),
                Arrays.asList(new Path[]{subImage1})
        );
        masker.run();
        getReadedBarcodeFromVideo(maskedDestination, BarcodeFormat.CODE_39).stream().forEach(System.out::println);
    }

    @Test
    public void can_remove_multiple_subimage_of_subsequent_frames() throws Exception {
        String maskedDestination = "build/recording_from_barcode_at_4x.masked.4.mp4";

        Path subImage1 = Paths.get("src/test/resources/barcode-subimage-6.png");
        VideoMasker masker = new VideoMasker(
                Paths.get(barcodeVideo),
                Paths.get(maskedDestination),
                Arrays.asList(new Path[]{subImage1})
        );
        masker.run();
        getReadedBarcodeFromVideo(maskedDestination, BarcodeFormat.CODE_39).stream().forEach(System.out::println);
    }

    @Test
    public void can_remove_subimage_of_qrcode() throws Exception {
        String maskedDestination = "build/recording_from_qrcode_at_4x.masked.1.mp4";

        Path subImage1 = Paths.get("src/test/resources/barcode-subimage-1.png");
        VideoMasker masker = new VideoMasker(
                Paths.get(qrCodeVideo),
                Paths.get(maskedDestination),
                Arrays.asList(new Path[]{subImage1})
        );
        masker.run();
        //TODO: Check why the frame gets dropped.
    }
}
