package acceptance;

import org.junit.Ignore;
import org.junit.Test;
import tdl.anonymize.video.VideoMasker;
import tdl.record.image.input.GenerateInputWithMatrixOfBarcodes;
import tdl.record.image.output.OutputToBarcodeMatrixReader;
import tdl.record.time.FakeTimeSource;
import tdl.record.video.VideoPlayer;
import tdl.record.video.VideoPlayerException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class CanMaskSubImagesTest {

    @Test
    public void should_not_have_false_positives() throws Exception {

        String destination = "build/recording_not_changed_after_false_matches.mp4";
        Path subImage = Paths.get("src/test/resources/images/barcode-false-positive.png");
        VideoMasker masker = new VideoMasker(
                Paths.get(GenerateInputWithMatrixOfBarcodes.BARCODE_VIDEO_PATH),
                Paths.get(destination),
                Collections.singletonList(subImage)
        );
        masker.run(1);

        List<OutputToBarcodeMatrixReader.TimestampedPayload> tamperedBarcodes
                = getReadedBarcodeFromVideo(destination).stream()
                        .filter(CanMaskSubImagesTest::isPayloadOutOfOrder)
                        .collect(Collectors.toList());
        assertThat(tamperedBarcodes.size(), is(0));
    }
    
    @Test
    public void should_have_multiple_true_positives_in_multiple_frames() throws Exception {
        String destination = "build/recording-masked.4.mp4";
        Path subImage1 = Paths.get("src/test/resources/rec_barcode_matrix/subimage-1.png");
        Path subImage2 = Paths.get("src/test/resources/rec_barcode_matrix/subimage-2.png");
        VideoMasker masker = new VideoMasker(
                Paths.get(GenerateInputWithMatrixOfBarcodes.BARCODE_VIDEO_PATH),
                Paths.get(destination),
                Arrays.asList(subImage1, subImage2)
        );
        masker.run(1);

        List<OutputToBarcodeMatrixReader.TimestampedPayload> tamperedBarcodes
                = getReadedBarcodeFromVideo(destination).stream()
                        .filter(CanMaskSubImagesTest::isPayloadOutOfOrder)
                        .collect(Collectors.toList());
        assertThat(tamperedBarcodes.size(), is(2));
        assertDecodedBarcode(tamperedBarcodes.get(0),
                7L, "", "1200", "1200", "");
        assertDecodedBarcode(tamperedBarcodes.get(1),
                9L, "", "", "", "");

    }

    @Test
    public void should_be_able_to_read_ahead_frames() throws Exception {
        String destination = "build/recording-masked.5.mp4";
        Path subImage1 = Paths.get("src/test/resources/rec_barcode_static/subimage-1.png");
        VideoMasker masker = new VideoMasker(
                Paths.get(GenerateInputWithMatrixOfBarcodes.BARCODE_WITH_STATIC_PATH),
                Paths.get(destination),
                Collections.singletonList(subImage1)
        );
        masker.run(3);

        List<OutputToBarcodeMatrixReader.TimestampedPayload> tamperedBarcodes
                = getReadedBarcodeFromVideo(destination).stream()
                .filter(CanMaskSubImagesTest::isPayloadOutOfOrder)
                .collect(Collectors.toList());
        assertThat(tamperedBarcodes.size(), is(3));
        assertDecodedBarcode(tamperedBarcodes.get(0),
                3L, "999", "", "", "999");
        assertDecodedBarcode(tamperedBarcodes.get(1),
                4L, "999", "", "", "999");
        assertDecodedBarcode(tamperedBarcodes.get(2),
                5L, "999", "", "", "999");

    }

    @SuppressWarnings("SameParameterValue")
    private void assertDecodedBarcode(OutputToBarcodeMatrixReader.TimestampedPayload frame, long timestamp,
                                      String topLeft, String topRight, String bottomLeft, String bottomRight) {
        assertThat("["+timestamp+"] videoTimestamp", frame.videoTimestamp, is(timestamp));
        assertThat("["+timestamp+"] topLeftPayload", frame.topLeftPayload, is(topLeft));
        assertThat("["+timestamp+"] topRightPayload", frame.topRightPayload, is(topRight));
        assertThat("["+timestamp+"] bottomLeftPayload", frame.bottomLeftPayload, is(bottomLeft));
        assertThat("["+timestamp+"] bottomRightPayload", frame.bottomRightPayload, is(bottomRight));
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
