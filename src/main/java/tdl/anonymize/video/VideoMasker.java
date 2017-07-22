package tdl.anonymize.video;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.opencv_core.Mat;
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
 */
@Slf4j
public class VideoMasker implements AutoCloseable {

    private final Path inputPath;
    private final Path outputPath;
    private final List<ImageMasker> subImageMaskers;

    private int counter = 0;

    //TODO: Wrap frame grabber exception
    public VideoMasker(Path inputPath, Path outputPath, List<Path> subImagePaths) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.subImageMaskers = subImagePaths.stream().map((path) -> {
            try {
                return new ImageMasker(path);
            } catch (ImageMaskerException ex) {
                return null;
            }
        })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void run() throws FrameGrabber.Exception, ImageMaskerException, FrameRecorder.Exception {
        try (FFmpegFrameGrabber grabber = createGrabber()) {
            grabber.start();
            try (FFmpegFrameRecorder recorder = createRecorder(grabber)) {
                recorder.start();
                Frame frame;
                while ((frame = grabber.grab()) != null) {
                    Frame editedFrame = maskFrame(frame);
                    recorder.setTimestamp(grabber.getTimestamp());
                    recorder.record(editedFrame);
                }
            }
        }
    }

    private static final ToMat FRAME_CONVERTER = new ToMat();

    private Frame maskFrame(Frame frame) {
        long timeBefore = System.nanoTime();
        
        Mat image = FRAME_CONVERTER.convert(frame);
        subImageMaskers.forEach((masker) -> {
            masker.mask(image);
        });
        Frame editedFrame = FRAME_CONVERTER.convert(image);
        
        long timeAfter = System.nanoTime();
        System.out.printf("Frame processed in: %d ms\n", ((timeAfter - timeBefore) / 1000000));
        return editedFrame;
    }

    public int getCount() {
        return counter;
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

    @Override
    public void close() throws Exception {
        this.subImageMaskers.stream().forEach((masker) -> {
            try {
                masker.close();
            } catch (Exception ex) {
                //Do nothing
            }
        });
    }

}
