package tdl.anonymize.video;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.opencv_core.Mat;
import static org.bytedeco.javacpp.opencv_highgui.destroyAllWindows;
import static org.bytedeco.javacpp.opencv_highgui.imshow;
import static org.bytedeco.javacpp.opencv_highgui.waitKey;
import org.bytedeco.javacpp.opencv_videoio;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameRecorder;
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat;
import tdl.anonymize.image.ImageMasker;
import tdl.anonymize.image.ImageMaskerException;

/**
 * Receives a path to a video and then mask.
 *
 * https://github.com/bytedeco/javacv-examples/blob/e3fc16b3c1da8a284637984c7a813fa1007212a8/OpenCV2_Cookbook/src/main/scala/opencv2_cookbook/chapter11/VideoProcessor.scala
 *
 * @author Petra Barus <petra.barus@gmail.com>
 */
@Slf4j
public class VideoMasker {

    private final Path inputPath;
    private final Path outputPath;
    private final List<Path> subImagePaths;

    //TODO: Wrap frame grabber exception
    public VideoMasker(Path inputPath, Path outputPath, List<Path> subImagePaths) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.subImagePaths = subImagePaths;
    }

    public void run() throws FrameGrabber.Exception, ImageMaskerException, FrameRecorder.Exception {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputPath.toFile())) {
            grabber.start();
            try (FFmpegFrameRecorder recorder = createRecorder(grabber)) {
                recorder.start();
                log.info("Frame count " + grabber.getLengthInFrames());
                Frame frame;
                ToMat frameConverter = new ToMat();
                int currentFrame = 0;
                while ((frame = grabber.grab()) != null) {
                    log.info("Frame #" + (currentFrame++));
                    Mat image = frameConverter.convert(frame);
                    ImageMasker masker = new ImageMasker(image);
                    subImagePaths.stream().forEach((subImage) -> {
                        try {
                            masker.findSubImageAndRemoveAllOccurences(subImage);
                        } catch (ImageMaskerException ex) {
                            log.error("Cannot find image in frame");
                        }
                    });
                    Mat editedImage = masker.getImage();
                    Frame editedFrame = frameConverter.convert(editedImage);
                    recorder.setTimestamp(grabber.getTimestamp());
                    recorder.record(editedFrame);
                }
                log.info("Saving new video");
            }
            log.info("Closing old video");
        }
    }

    private FFmpegFrameGrabber createGrabber() {
        return new FFmpegFrameGrabber(inputPath.toFile());
    }

    private FFmpegFrameRecorder createRecorder(FFmpegFrameGrabber grabber) {
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(
                outputPath.toFile(),
                grabber.getImageWidth(),
                grabber.getImageHeight(),
                2
        );
        recorder.setVideoCodec(grabber.getVideoCodec());
        recorder.setFormat(grabber.getFormat());
        recorder.setFrameRate(grabber.getFrameRate());
        recorder.setSampleFormat(grabber.getSampleFormat());
        recorder.setSampleRate(grabber.getSampleRate());
        recorder.setAudioCodec(grabber.getAudioCodec());
        return recorder;
    }

}
