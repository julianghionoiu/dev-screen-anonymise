package tdl.anonymize.video;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameRecorder;
import org.junit.Test;
import tdl.anonymize.image.ImageMaskerException;

public class VideoMaskerTest {

    @Test
    public void test() throws FrameGrabber.Exception, ImageMaskerException, FrameRecorder.Exception, Exception {
        Path inputPath = Paths.get("./src/test/resources/video/sample-recording.mp4");
        Path outputPath = Paths.get("./output.mp4");
        Path subImagePath = Paths.get("./src/test/resources/video/subimage-2.png");
        Path subImagesPath[] = new Path[]{subImagePath};
        VideoMasker masker = new VideoMasker(
                inputPath,
                outputPath,
                Arrays.asList(subImagesPath)
        );
        masker.run();
    }
}
