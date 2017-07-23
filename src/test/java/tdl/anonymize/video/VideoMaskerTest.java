package tdl.anonymize.video;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class VideoMaskerTest {

    @Test
    public void test() throws Exception {
        Path inputPath = Paths.get("./src/test/resources/video/sample-recording.mp4");
        Path outputPath = Paths.get("./output.mp4");
        Path subImagePath = Paths.get("./src/test/resources/video/subimage-2.png");
        Path subImagesPath[] = new Path[]{subImagePath};
        VideoMasker masker = new VideoMasker(
                inputPath,
                outputPath,
                Arrays.asList(subImagesPath)
        );
        masker.run(3);
    }
}
