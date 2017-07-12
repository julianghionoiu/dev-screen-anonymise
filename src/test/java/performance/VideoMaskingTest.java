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
        int count0 = runWithoutMasking();
        long baseline = endTimer("run_without_masking");

        startTimer("run_with_one_masking");
        int count1 = runWithOneMasking();
        long duration1 = endTimer("run_with_one_masking");

        startTimer("run_with_two_maskings");
        int count2 = runWithTwoMaskings();
        long duration2 = endTimer("run_with_two_maskings");
        
        startTimer("run_with_three_maskings");
        int count3 = runWithThreeMaskings();
        long duration3 = endTimer("run_with_three_maskings");

        double ratio1 = calculateRatio(duration1, baseline);
        System.out.printf("%d %f\n", count1, ratio1);
        double ratio2 = calculateRatio(duration2, baseline);
        System.out.printf("%d %f\n", count2, ratio2);
        double ratio3 = calculateRatio(duration3, baseline);
        System.out.printf("%d %f\n", count3, ratio3);
    }

    private int runWithoutMasking() throws Exception {
        String destination = "build/recording-masked.perftest-1.mp4";
        VideoMasker masker = new VideoMasker(
                Paths.get(VIDEO_INPUT_PATH),
                Paths.get(destination),
                Arrays.asList(new Path[]{})
        );
        masker.run();
        return masker.getCount();
    }

    private int runWithOneMasking() throws Exception {
        String destination = "build/recording-masked.perftest-2.mp4";
        Path subImage1 = Paths.get("src/test/resources/subimage-1.png");
        VideoMasker masker = new VideoMasker(
                Paths.get(VIDEO_INPUT_PATH),
                Paths.get(destination),
                Arrays.asList(new Path[]{subImage1})
        );
        masker.run();
        return masker.getCount();
    }
    
    private int runWithTwoMaskings() throws Exception {
        String destination = "build/recording-masked.perftest-2.mp4";
        Path subImage1 = Paths.get("src/test/resources/subimage-1.png");
        Path subImage2 = Paths.get("src/test/resources/subimage-2.png");
        VideoMasker masker = new VideoMasker(
                Paths.get(VIDEO_INPUT_PATH),
                Paths.get(destination),
                Arrays.asList(new Path[]{subImage1, subImage2})
        );
        masker.run();
        return masker.getCount();
    }
    
    private int runWithThreeMaskings() throws Exception {
        String destination = "build/recording-masked.perftest-3.mp4";
        Path subImage1 = Paths.get("src/test/resources/subimage-1.png");
        Path subImage2 = Paths.get("src/test/resources/subimage-2.png");
        Path subImage3 = Paths.get("src/test/resources/subimage-3.png");
        VideoMasker masker = new VideoMasker(
                Paths.get(VIDEO_INPUT_PATH),
                Paths.get(destination),
                Arrays.asList(new Path[]{subImage1, subImage2, subImage3})
        );
        masker.run();
        return masker.getCount();
    }
}
