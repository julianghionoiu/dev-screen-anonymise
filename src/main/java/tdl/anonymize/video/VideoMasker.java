package tdl.anonymize.video;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat;
import tdl.anonymize.image.ImageMasker;
import tdl.anonymize.image.ImageMaskerException;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Receives a path to a video and then findMatchingPoints.
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

                // Start with no triggered matchers and no previous matching points
                ProcessedFrame previousReadAheadFrame = new ProcessedFrame(null, new HashMap<>());
                HashMap<ImageMasker, List<opencv_core.Point>> reusableMatches = new HashMap<>();


                while (currentFrameIndex < totalFrames) {
                    int framesToReadAhead = Math.min(readAheadStep, totalFrames - currentFrameIndex);
                    int normalFramesToRead = framesToReadAhead - 1;

                    long timeBefore = System.nanoTime();

                    //Skip the normal frames
                    for (int i = 0; i < normalFramesToRead; i++) {
                        readAheadGrabber.grab();
                    }
                    reusableMatches.clear();
                    ProcessedFrame editedReadAheadFrame = processFrame(readAheadGrabber.grab(), allSubImageMaskers, reusableMatches);

                    //Compute active maskers
                    activeImageMaskers.clear();
                    for (ImageMasker imageMasker : allSubImageMaskers) {
                        if (previousReadAheadFrame.triggeredMaskers.containsKey(imageMasker) ||
                                editedReadAheadFrame.triggeredMaskers.containsKey(imageMasker)) {
                            activeImageMaskers.add(imageMasker);
                        }
                    }

                    //Matches that have not changed since previous run do not need to be matched again
                    for (ImageMasker imageMasker : allSubImageMaskers) {
                        if (previousReadAheadFrame.triggeredMaskers.containsKey(imageMasker) &&
                                editedReadAheadFrame.triggeredMaskers.containsKey(imageMasker) &&
                                samePoints(previousReadAheadFrame.triggeredMaskers.get(imageMasker),
                                        editedReadAheadFrame.triggeredMaskers.get(imageMasker))) {
                            reusableMatches.put(imageMasker, previousReadAheadFrame.triggeredMaskers.get(imageMasker));
                        }
                    }

                    //Mask normal frames
                    for (int i = 0; i < normalFramesToRead; i++) {
                        ProcessedFrame editedNormalFrame = processFrame(grabber.grab(), activeImageMaskers, reusableMatches);
                        recorder.record(editedNormalFrame.frame);
                        currentFrameIndex += 1;
                    }

                    //Record the read ahead frame
                    recorder.record(editedReadAheadFrame.frame);
                    currentFrameIndex += 1;

                    //Align the normal grabbers
                    grabber.grab();

                    //Store the previous frame
                    previousReadAheadFrame.dispose();
                    previousReadAheadFrame = editedReadAheadFrame;

                    long timeAfter = System.nanoTime();
                    long durationMs = (timeAfter - timeBefore) / 1000000;
                    System.out.printf("Processing speed: %d ms per frame\n", durationMs/framesToReadAhead);
                }
            }
        }
    }

    private static boolean samePoints(List<opencv_core.Point> prevPoints, List<opencv_core.Point> currentPoints) {
        if (prevPoints.size() != currentPoints.size()) {
            return false;
        }

        for (int i = 0; i < prevPoints.size(); i++) {
            opencv_core.Point prevPoint = prevPoints.get(i);
            opencv_core.Point currPoint = currentPoints.get(i);

            if (prevPoint.x() != currPoint.x()) {
                return false;
            }
            if (prevPoint.y() != currPoint.y()) {
                return false;
            }
        }

        return true;
    }

    private ProcessedFrame processFrame(Frame frame, List<ImageMasker> subImageMaskers,
                                        HashMap<ImageMasker,
                                        List<opencv_core.Point>> reusableMatches) throws FrameGrabber.Exception {
        String reusingMatches = reusableMatches.keySet().stream().map(ImageMasker::getName)
                .collect(Collectors.joining(", ", "[", "]"));
        System.out.println("activeImageMaskers.size() = " + subImageMaskers.size() + ", reusing matches: "+reusingMatches);

        Map<ImageMasker, List<opencv_core.Point>> matchedMaskers = new HashMap<>();
        Mat mat = FRAME_CONVERTER.convert(frame);

        if (mat != null) {
            for (ImageMasker masker : subImageMaskers) {
                List<opencv_core.Point> matchingPoints;
                if (reusableMatches.containsKey(masker)) {
                    matchingPoints = reusableMatches.get(masker);
                } else {
                    matchingPoints = masker.findMatchingPoints(mat);
                }
                masker.blurPoints(matchingPoints, mat);

                if (matchingPoints.size() > 0) {
                    matchedMaskers.put(masker, matchingPoints);
                }
            }
        }

        Frame editedFrame = FRAME_CONVERTER.convert(mat);
        return new ProcessedFrame(editedFrame, matchedMaskers);
    }

    private static class ProcessedFrame {
        private final Frame frame;
        private final Map<ImageMasker, List<opencv_core.Point>> triggeredMaskers;

        ProcessedFrame(Frame frame, Map<ImageMasker, List<opencv_core.Point>> triggeredMaskers) {
            this.frame = frame;
            this.triggeredMaskers = triggeredMaskers;
        }

        void dispose() {
            this.triggeredMaskers.values().stream().flatMap(Collection::stream).forEach(Pointer::close);
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
