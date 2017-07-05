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

import tdl.anonymize.video.VideoMasker;
import tdl.record.video.VideoPlayerException;

public class CanMaskSubImagesTest {

    @Test
    public void should_not_have_false_positives() throws Exception {

        String destination = "build/recording_not_changed_after_false_matches.mp4";
        Path subImage = Paths.get("src/test/resources/barcode-false-positive.png");
        VideoMasker masker = new VideoMasker(
                Paths.get(GenerateInputWithMatrixOfBarcodes.OUTPUT_PATH),
                Paths.get(destination),
                Collections.singletonList(subImage)
        );
        masker.run();

        List<OutputToBarcodeMatrixReader.TimestampedPayload> tamperedBarcodes =
                getReadedBarcodeFromVideo(destination).stream()
                .peek(System.out::println)
                .filter(CanMaskSubImagesTest::isPayloadOutOfOrder)
                .collect(Collectors.toList());
        assertThat(tamperedBarcodes.size(), is(0));
    }

    //~~~~~~~~~~ Helpers


    private static boolean isPayloadOutOfOrder(OutputToBarcodeMatrixReader.TimestampedPayload payload) {
        return !isPayloadConsistent(payload);
    }

    private static boolean isPayloadConsistent(OutputToBarcodeMatrixReader.TimestampedPayload payload) {
        int timebaseIncrement = 200;
        long expectedPayload = (payload.videoTimestamp - 1) * timebaseIncrement;

        System.out.println(expectedPayload);
        try {
            return  (Long.parseLong(payload.topLeftPayload) == expectedPayload) &&
                    (Long.parseLong(payload.topRightPayload) == expectedPayload) &&
                    (Long.parseLong(payload.bottomLeftPayload) == expectedPayload) &&
                    (Long.parseLong(payload.bottomRightPayload) == expectedPayload);
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
