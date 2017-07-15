package tdl.anonymize;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameRecorder;
import tdl.anonymize.image.ImageMaskerException;
import tdl.anonymize.video.VideoMasker;

/**
 * This will receive list of paths. The first path will be the main image,
 * followed by paths of sub image to be censored.
 */
@Slf4j
public class AnonymizeApp {

    @Parameter()
    public List<String> paths = new ArrayList<>();

    @Parameter(names = {"-o", "--output"}, description = "The path to the recording file")
    public String destinationPath = "./output.mp4";

    public static void main(String[] args) throws ImageMaskerException, FrameGrabber.Exception, FrameRecorder.Exception, Exception {
        AnonymizeApp main = new AnonymizeApp();
        JCommander.newBuilder()
                .addObject(main)
                .build()
                .parse(args);
        main.run();
    }

    public void run() throws ImageMaskerException, FrameGrabber.Exception, FrameRecorder.Exception, Exception {
        if (paths.size() < 2) {
            throw new RuntimeException("Parameter has to be at least 2");
        }
        Path videoPath = Paths.get(paths.get(0));
        paths.remove(0);
        List<Path> subImagePaths = paths.stream().map(Paths::get).collect(Collectors.toList());
        Path destination = Paths.get(destinationPath);
        
        VideoMasker masker = new VideoMasker(videoPath, destination, subImagePaths);
        masker.run();
    }
}
