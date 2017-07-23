package tdl.anonymize.video;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat;
import tdl.anonymize.image.ImageMasker;
import tdl.anonymize.image.ImageMaskerException;

import java.nio.file.Path;
import java.util.*;
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
    private final List<ImageMasker> allSubImageMaskers;

    //TODO: Wrap frame grabber exception
    public VideoMasker(Path inputPath, Path outputPath, List<Path> subImagePaths) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.allSubImageMaskers = subImagePaths.stream().map((path) -> {
            try {
                return new ImageMasker(path);
            } catch (ImageMaskerException ex) {
                return null;
            }
        })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void run(int readAheadStep) throws FrameGrabber.Exception, ImageMaskerException, FrameRecorder.Exception {
        try (FFmpegFrameGrabber grabber = createGrabber(); FFmpegFrameGrabber readAheadGrabber = createGrabber()) {
            grabber.start();
            readAheadGrabber.start();
            try (FFmpegFrameRecorder recorder = createRecorder(grabber)) {
                recorder.start();
                int currentFrameIndex = 0;
                int totalFrames = grabber.getLengthInFrames();
                List<ImageMasker> activeImageMaskers = new ArrayList<>();
                ProcessedFrame previousReadAheadFrame = new ProcessedFrame(null, new HashSet<>());

                // All matchers are false

                while (currentFrameIndex < totalFrames) {
                    int framesToReadAhead = Math.min(readAheadStep, totalFrames - currentFrameIndex);
                    int normalFramesToRead = framesToReadAhead - 1;

                    long timeBefore = System.nanoTime();

                    //Skip the normal frames
                    for (int i = 0; i < normalFramesToRead; i++) {
                        readAheadGrabber.grab();
                    }
                    ProcessedFrame editedReadAheadFrame = processFrame(readAheadGrabber.grab(), allSubImageMaskers);

                    //Compute active maskers
                    activeImageMaskers.clear();
                    for (ImageMasker imageMasker : allSubImageMaskers) {
                        if (previousReadAheadFrame.triggeredMaskers.contains(imageMasker) ||
                                editedReadAheadFrame.triggeredMaskers.contains(imageMasker)) {
                            activeImageMaskers.add(imageMasker);
                        }
                    }
                    System.out.println("activeImageMaskers.size() = " + activeImageMaskers.size());

                    //Mask normal frames
                    for (int i = 0; i < normalFramesToRead; i++) {
                        ProcessedFrame editedNormalFrame = processFrame(grabber.grab(), activeImageMaskers);
                        recorder.record(editedNormalFrame.frame);
                        currentFrameIndex += 1;
                    }

                    //Record the read ahead frame
                    recorder.record(editedReadAheadFrame.frame);
                    currentFrameIndex += 1;

                    //Align the normal grabbers
                    grabber.grab();


                    long timeAfter = System.nanoTime();
                    long durationMs = (timeAfter - timeBefore) / 1000000;
                    System.out.printf("Processing speed: %d ms per frame\n", durationMs/framesToReadAhead);
                }
            }
        }
    }

    private ProcessedFrame processFrame(Frame frame, List<ImageMasker> subImageMaskers) throws FrameGrabber.Exception {
        Set<ImageMasker> matchedMaskers = new HashSet<>();
        Mat mat = FRAME_CONVERTER.convert(frame);
        for (ImageMasker masker : subImageMaskers) {
            int matches = masker.mask(mat);

            if (matches > 0) {
                matchedMaskers.add(masker);
            }
        }

        Frame editedFrame = FRAME_CONVERTER.convert(mat);
        return new ProcessedFrame(editedFrame, matchedMaskers);
    }

    private static class ProcessedFrame {
        private final Frame frame;
        private final Set<ImageMasker> triggeredMaskers;

        ProcessedFrame(Frame frame, Set<ImageMasker> triggeredMaskers) {
            this.frame = frame;
            this.triggeredMaskers = triggeredMaskers;
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
        this.allSubImageMaskers.forEach((masker) -> {
            try {
                masker.close();
            } catch (Exception ex) {
                //Do nothing
            }
        });
    }

}
