package acceptance;

import java.io.IOException;

import org.junit.Test;
import tdl.record.image.input.*;
import tdl.record.image.output.OutputToBarcodeMatrixReader;
import tdl.record.time.FakeTimeSource;
import tdl.record.video.VideoPlayer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static java.lang.Math.abs;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.stream.Collectors;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertTrue;

import tdl.anonymize.video.VideoMasker;
import tdl.record.video.VideoPlayerException;

public class CanMaskSubImagesTest {

    private static final String VIDEO_INPUT_PATH = GenerateInputWithMatrixOfBarcodes.OUTPUT_PATH;

    @Test
    public void should_not_have_false_positives() throws Exception {

        String destination = "build/recording_not_changed_after_false_matches.mp4";
        Path subImage = Paths.get("src/test/resources/barcode-false-positive.png");
        VideoMasker masker = new VideoMasker(
                Paths.get(VIDEO_INPUT_PATH),
                Paths.get(destination),
                Collections.singletonList(subImage)
        );
        masker.run();

        List<OutputToBarcodeMatrixReader.TimestampedPayload> tamperedBarcodes
                = getReadedBarcodeFromVideo(destination).stream()
                        .peek(System.out::println)
                        .filter(CanMaskSubImagesTest::isPayloadOutOfOrder)
                        .collect(Collectors.toList());
        assertThat(tamperedBarcodes.size(), is(0));
    }

    @Test
    public void should_have_one_true_positives_in_one_frame() throws Exception {
        String destination = "build/recording-masked.1.mp4";
        Path subImage = Paths.get("src/test/resources/subimage-1.png");
        VideoMasker masker = new VideoMasker(
                Paths.get(VIDEO_INPUT_PATH),
                Paths.get(destination),
                Collections.singletonList(subImage)
        );
        masker.run();

        List<OutputToBarcodeMatrixReader.TimestampedPayload> tamperedBarcodes
                = getReadedBarcodeFromVideo(destination).stream()
                        .collect(Collectors.toList());

        OutputToBarcodeMatrixReader.TimestampedPayload tamperedFrame = tamperedBarcodes.get(8);
        long expected = getExpectedPayload(tamperedFrame);
        assertTrue(tamperedFrame.topLeftPayload.isEmpty());
        assertTrue(tamperedFrame.topRightPayload.isEmpty());
        assertTrue(Long.parseLong(tamperedFrame.bottomLeftPayload) == expected);
        assertTrue(Long.parseLong(tamperedFrame.bottomRightPayload) == expected);
    }

    @Test
    public void should_have_multiple_true_positives_in_one_frame_using_one_subimage() throws Exception {
        String destination = "build/recording-masked.2.mp4";
        Path subImage = Paths.get("src/test/resources/subimage-2.png");
        VideoMasker masker = new VideoMasker(
                Paths.get(VIDEO_INPUT_PATH),
                Paths.get(destination),
                Collections.singletonList(subImage)
        );
        masker.run();

        List<OutputToBarcodeMatrixReader.TimestampedPayload> tamperedBarcodes
                = getReadedBarcodeFromVideo(destination).stream()
                        .collect(Collectors.toList());

        OutputToBarcodeMatrixReader.TimestampedPayload tamperedFrame = tamperedBarcodes.get(8);
        long expected = getExpectedPayload(tamperedFrame);
        assertTrue(tamperedFrame.bottomLeftPayload.isEmpty());
        assertTrue(tamperedFrame.topRightPayload.isEmpty());
        assertTrue(Long.parseLong(tamperedFrame.topLeftPayload) == expected);
        assertTrue(Long.parseLong(tamperedFrame.bottomRightPayload) == expected);
    }

    @Test
    public void should_have_multiple_true_positives_in_one_frame_using_two_subimages() throws Exception {
        String destination = "build/recording-masked.3.mp4";
        Path subImage1 = Paths.get("src/test/resources/subimage-2.png");
        Path subImage2 = Paths.get("src/test/resources/subimage-3.png");
        VideoMasker masker = new VideoMasker(
                Paths.get(VIDEO_INPUT_PATH),
                Paths.get(destination),
                Arrays.asList(new Path[]{subImage1, subImage2})
        );
        masker.run();

        List<OutputToBarcodeMatrixReader.TimestampedPayload> tamperedBarcodes
                = getReadedBarcodeFromVideo(destination).stream()
                        .collect(Collectors.toList());

        OutputToBarcodeMatrixReader.TimestampedPayload tamperedFrame = tamperedBarcodes.get(8);
        assertTrue(tamperedFrame.topLeftPayload.isEmpty());
        assertTrue(tamperedFrame.topRightPayload.isEmpty());
        assertTrue(tamperedFrame.bottomLeftPayload.isEmpty());
        assertTrue(tamperedFrame.bottomRightPayload.isEmpty());
    }

    @Test
    public void should_have_multiple_true_positives_in_multiple_frames() throws Exception {
        String destination = "build/recording-masked.4.mp4";
        Path subImage1 = Paths.get("src/test/resources/subimage-4.png");
        VideoMasker masker = new VideoMasker(
                Paths.get(VIDEO_INPUT_PATH),
                Paths.get(destination),
                Arrays.asList(new Path[]{subImage1})
        );
        masker.run();

        List<OutputToBarcodeMatrixReader.TimestampedPayload> tamperedBarcodes
                = getReadedBarcodeFromVideo(destination).stream()
                        .filter(CanMaskSubImagesTest::isPayloadOutOfOrder)
                        .collect(Collectors.toList());
        assertThat(tamperedBarcodes.size(), is(2));
    }

    //~~~~~~~~~~ Helpers
    private static boolean isPayloadOutOfOrder(OutputToBarcodeMatrixReader.TimestampedPayload payload) {
        return !isPayloadConsistent(payload);
    }

    private static long getExpectedPayload(OutputToBarcodeMatrixReader.TimestampedPayload payload) {
        int timebaseIncrement = 200;
        return (payload.videoTimestamp - 1) * timebaseIncrement;
    }

    private static boolean isPayloadConsistent(OutputToBarcodeMatrixReader.TimestampedPayload payload) {
        long expectedPayload = getExpectedPayload(payload);
        System.out.println(expectedPayload);
        try {
            return (Long.parseLong(payload.topLeftPayload) == expectedPayload)
                    && (Long.parseLong(payload.topRightPayload) == expectedPayload)
                    && (Long.parseLong(payload.bottomLeftPayload) == expectedPayload)
                    && (Long.parseLong(payload.bottomRightPayload) == expectedPayload);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static List<OutputToBarcodeMatrixReader.TimestampedPayload> getReadedBarcodeFromVideo(String path) throws VideoPlayerException, InterruptedException, IOException {
        OutputToBarcodeMatrixReader barcodeReader = new OutputToBarcodeMatrixReader(new FakeTimeSource());
        VideoPlayer videoPlayer = new VideoPlayer(barcodeReader, new FakeTimeSource());
        videoPlayer.open(path);

        videoPlayer.play();
        videoPlayer.close();
        return barcodeReader.getDecodedBarcodes();
    }

}
