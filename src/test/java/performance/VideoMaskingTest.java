package performance;

import acceptance.CanMaskSubImagesTest;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.bytedeco.javacv.FrameGrabber;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import tdl.anonymize.video.VideoMasker;
import tdl.record.image.input.GenerateInputWithMatrixOfBarcodes;
import tdl.record.image.output.OutputToBarcodeMatrixReader;

public class VideoMaskingTest {

    private static final String VIDEO_INPUT_PATH = GenerateInputWithMatrixOfBarcodes.OUTPUT_PATH;

    private static Map<String, Long> timer = new HashMap<>();

    private static void startTimer(String id) {
        timer.put(id, System.nanoTime());
    }

    private static long endTimer(String id) {
        long end = System.nanoTime();
        long start = timer.get(id);
        return end - start;
    }

    private static double calculateRatio(long n1, long n2) {
        return ((double) n1 - (double) n2) / (double) n2;
    }

    @Test
    public void run() throws Exception {
        startTimer("run_without_masking");
        runWithoutMasking();
        long baseline = endTimer("run_without_masking");

        startTimer("run_with_one_masking");
        runWithOneMasking();
        long duration1 = endTimer("run_with_one_masking");

        double ratio1 = calculateRatio(duration1, baseline);
        System.out.printf("%f\n", ratio1);
        
        startTimer("run_with_two_masking");
        runWithTwoMasking();
        long duration2 = endTimer("run_with_two_masking");

        double ratio2 = calculateRatio(duration2, baseline);
        System.out.printf("%f\n", ratio2);
    }

    private void runWithoutMasking() throws Exception {
        String destination = "build/recording-masked.perftest-1.mp4";
        VideoMasker masker = new VideoMasker(
                Paths.get(VIDEO_INPUT_PATH),
                Paths.get(destination),
                Arrays.asList(new Path[]{})
        );
        masker.run();
    }

    private void runWithOneMasking() throws Exception {
        String destination = "build/recording-masked.perftest-2.mp4";
        Path subImage1 = Paths.get("src/test/resources/subimage-1.png");
        VideoMasker masker = new VideoMasker(
                Paths.get(VIDEO_INPUT_PATH),
                Paths.get(destination),
                Arrays.asList(new Path[]{subImage1})
        );
        masker.run();
    }
    
    private void runWithTwoMasking() throws Exception {
        String destination = "build/recording-masked.perftest-2.mp4";
        Path subImage1 = Paths.get("src/test/resources/subimage-1.png");
        Path subImage2 = Paths.get("src/test/resources/subimage-2.png");
        VideoMasker masker = new VideoMasker(
                Paths.get(VIDEO_INPUT_PATH),
                Paths.get(destination),
                Arrays.asList(new Path[]{subImage1, subImage2})
        );
        masker.run();
    }
}
