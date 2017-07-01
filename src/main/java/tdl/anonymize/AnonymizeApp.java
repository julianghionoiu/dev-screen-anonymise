package tdl.anonymize;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import tdl.anonymize.image.ImageMasker;
import tdl.anonymize.image.ImageMaskerException;

/**
 * This will receive list of paths. The first path will be the main image,
 * followed by paths of sub image to be censored.
 */
@Slf4j
public class AnonymizeApp {

    @Parameter()
    public List<String> paths = new ArrayList<>();

    @Parameter(names = {"-o", "--output"}, description = "The path to the recording file")
    public String destinationPath = "./output.jpg";

    public static void main(String[] args) throws ImageMaskerException {
        AnonymizeApp main = new AnonymizeApp();
        JCommander.newBuilder()
                .addObject(main)
                .build()
                .parse(args);
        main.run();
    }

    public void run() throws ImageMaskerException {
        if (paths.size() < 2) {
            throw new RuntimeException("Parameter has to be at least 2");
        }
        String mainImage = paths.get(0);
        paths.remove(0);

        Path destination = Paths.get(destinationPath);
        ImageMasker masker = new ImageMasker(Paths.get(mainImage));
        List<String> subImages = paths;

        subImages.stream().forEach((subImage) -> {
            try {
                masker.findSubImageAndRemoveAllOccurences(Paths.get(subImage));
            } catch (ImageMaskerException ex) {
                log.warn("Cannot find image " + subImage);
            }
        });
        masker.showImage(); //DEBUG
        masker.saveImage(destination);
    }

    public static Path getOutputPath(Path output) {
        return null;
    }
}
