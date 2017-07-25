package tdl.anonymize;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import lombok.extern.slf4j.Slf4j;
import tdl.anonymize.video.VideoMasker;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * This will receive list of paths. The first path will be the main image,
 * followed by paths of sub image to be censored.
 */
@SuppressWarnings("FieldCanBeLocal")
@Slf4j
public class AnonymizeApp {

    @Parameter(names = {"-i", "--input"}, description = "The path to the input recording file", required = true)
    private String inputVideoPath;

    @Parameter(names = {"-o", "--output"}, description = "The path to the output recording file", required = true)
    private String outputVideoPath;

    @Parameter(names = {"-sd", "--subimages-dir"}, description = "Folder containing the subimages to match", required = true)
    private String subimagesDirPath;

    @Parameter(names = {"-th", "--matching-threshold"}, description = "The threshold used when matching subimages")
    private Double matchingThreshold = 0.96;


    @Parameter(names = {"-cbs", "--continuous-block-size"}, description = "Assume that the subimages will match in blocks")
    private Integer continuousBlockSize = 3;


    public static void main(String[] args) throws Exception {
        AnonymizeApp main = new AnonymizeApp();
        JCommander.newBuilder()
                .addObject(main)
                .build()
                .parse(args);
        main.run();
    }

    private void run() throws Exception {
        Path inputVideo = Paths.get(inputVideoPath);
        Path outputVideo = Paths.get(outputVideoPath);
        List<Path> subImages = new ArrayList<>();

        Path subimagesDir = Paths.get(subimagesDirPath);
        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(subimagesDir, "*.{png,jpg}")) {
            for (Path entry: stream) {
                subImages.add(entry);
            }
        }

        System.out.println("List of subimages:");
        subImages.forEach(System.out::println);

        VideoMasker masker = new VideoMasker(inputVideo, outputVideo, subImages, matchingThreshold);
        masker.run(continuousBlockSize);
    }
}
