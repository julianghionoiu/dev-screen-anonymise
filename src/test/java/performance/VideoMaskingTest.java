package performance;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import tdl.anonymize.video.VideoMasker;

public class VideoMaskingTest {

    private static final String VIDEO_INPUT_PATH = "./src/test/resources/real-recording.mp4";

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

        startTimer("run_with_two_maskings");
        int count2 = runWithTwoMaskings();
        long duration2 = endTimer("run_with_two_maskings");

        double ratio2 = calculateRatio(duration2, baseline);
        System.out.printf("%d %f\n", count2, ratio2);
    }

    private int runWithoutMasking() throws Exception {
        String destination = "build/real-recording.masked.1.mp4";
        VideoMasker masker = new VideoMasker(
                Paths.get(VIDEO_INPUT_PATH),
                Paths.get(destination),
                Arrays.asList(new Path[]{})
        );
        masker.run();
        return masker.getCount();
    }

    private int runWithTwoMaskings() throws Exception {
        String destination = "build/real-recording.masked.2.mp4";
        Path subImage1 = Paths.get("src/test/resources/real-recording-subimage-1.png");
        Path subImage2 = Paths.get("src/test/resources/real-recording-subimage-2.png");
        VideoMasker masker = new VideoMasker(
                Paths.get(VIDEO_INPUT_PATH),
                Paths.get(destination),
                Arrays.asList(new Path[]{subImage1, subImage2})
        );
        masker.run();
        return masker.getCount();
    }
}
