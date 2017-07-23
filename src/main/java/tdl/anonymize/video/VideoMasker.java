package tdl.anonymize.video;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat;
import tdl.anonymize.image.ImageMasker;
import tdl.anonymize.image.ImageMaskerException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
        try (FFmpegFrameGrabber grabber = createGrabber(); FFmpegFrameGrabber readAheadGrabber = createGrabber()) {
            grabber.start();
            readAheadGrabber.start();
            try (FFmpegFrameRecorder recorder = createRecorder(grabber)) {
                recorder.start();
                int readAheadStep = 2;
                int currentFrameIndex = 0;
                int totalFrames = grabber.getLengthInFrames();

                while (currentFrameIndex < totalFrames) {
                    int framesToProcess = Math.min(readAheadStep, totalFrames - currentFrameIndex);
                    long timeBefore = System.nanoTime();

                    Frame readAheadFrame;
                    {
                        readAheadGrabber.grab();
                        Frame frm = readAheadGrabber.grab();
                        Mat mat = FRAME_CONVERTER.convert(frm);
                        subImageMaskers.forEach((masker) -> masker.mask(mat));
                        readAheadFrame = FRAME_CONVERTER.convert(mat);
                    }

                    {
                        Frame frame = grabber.grab();
                        Mat mat = FRAME_CONVERTER.convert(frame);
                        subImageMaskers.forEach((masker) -> masker.mask(mat));
                        Frame editedFrame = FRAME_CONVERTER.convert(mat);
                        recorder.record(editedFrame);
                    }

                    recorder.record(readAheadFrame);

                    currentFrameIndex += framesToProcess;

                    //Align grabbers
                    if (framesToProcess > 1) {
                        grabber.grab();
                    }


                    long timeAfter = System.nanoTime();
                    long durationMs = (timeAfter - timeBefore) / 1000000;
                    System.out.printf("Processing speed: %d ms per frame\n", durationMs/framesToProcess);
                }
            }
        }
    }

    private static final ToMat FRAME_CONVERTER = new ToMat();


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
        this.subImageMaskers.forEach((masker) -> {
            try {
                masker.close();
            } catch (Exception ex) {
                //Do nothing
            }
        });
    }

}
