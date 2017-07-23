package performance;

import org.junit.Test;
import tdl.anonymize.video.VideoMasker;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

public class VideoMaskingTest {

    private static final String VIDEO_INPUT_PATH = "./src/test/resources/rec_real/real-recording.mp4";

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

        startTimer("run_with_three_maskings");
        runWithThreeMaskings();
        long durationWithProcessing = endTimer("run_with_three_maskings");

        double ratio = calculateRatio(durationWithProcessing, baseline);
        System.out.printf("Ratio: %f\n", ratio);
        assertThat(ratio, lessThan(4d));
    }

    private void runWithoutMasking() throws Exception {
        String destination = "build/real-recording.masked.1.mp4";
        VideoMasker masker = new VideoMasker(
                Paths.get(VIDEO_INPUT_PATH),
                Paths.get(destination),
                Arrays.asList(new Path[]{})
        );
        masker.run(3);
    }

    /**
     * To generate the images run:
     *  ffmpeg -i real-recording.mp4 -ss 00:00:14.000 -vframes 1 screen.png
     *
     */
    private void runWithThreeMaskings() throws Exception {
        String destination = "build/real-recording.masked.2.mp4";
        Path subImage1 = Paths.get("src/test/resources/rec_real/subimage-1.png");
        Path subImage2 = Paths.get("src/test/resources/rec_real/subimage-2.png");
        Path subImage3 = Paths.get("src/test/resources/rec_real/subimage-3.png");
        VideoMasker masker = new VideoMasker(
                Paths.get(VIDEO_INPUT_PATH),
                Paths.get(destination),
                Arrays.asList(subImage1, subImage2, subImage3)
        );
        masker.run(3);
    }
}
