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
        int batchSize = 2;
        List<Frame> rawFramesBuffer = new ArrayList<>();
        List<Frame> processedFrameBuffer = new ArrayList<>();
        try (FFmpegFrameGrabber grabber = createGrabber(); FFmpegFrameGrabber readAheadGrabber = createGrabber()) {
            grabber.start();
            readAheadGrabber.start();
            int currentFrameIndex = 0;
            int totalFrames = grabber.getLengthInFrames();
            try (FFmpegFrameRecorder recorder = createRecorder(grabber)) {
                recorder.start();
                while (currentFrameIndex < totalFrames) {
                    int framesToProcess = Math.min(batchSize, totalFrames - currentFrameIndex);
                    long timeBefore = System.nanoTime();

                    rawFramesBuffer.clear();
                    processedFrameBuffer.clear();

                    if (framesToProcess > 1) {
                        //Align the grabber
                        readAheadGrabber.grab();
                        Frame frame2 = readAheadGrabber.grab();
                        rawFramesBuffer.add(frame2);
                    }

                    Frame frame1 = grabber.grab();
                    rawFramesBuffer.add(frame1);


                    for (Frame frame : rawFramesBuffer) {
                        Mat mat = FRAME_CONVERTER.convert(frame);
                        subImageMaskers.forEach((masker) -> masker.mask(mat));
                        Frame editedFrame = FRAME_CONVERTER.convert(mat);
                        processedFrameBuffer.add(editedFrame);
                    }

                    for (int i = 0; i < framesToProcess; i++) {
                        recorder.record(processedFrameBuffer.get(i));
                    }

                    currentFrameIndex += framesToProcess;

                    //Align grabbers to new index
                    while (grabber.getFrameNumber() < currentFrameIndex) {
                        grabber.grab();
                    }

                    while (readAheadGrabber.getFrameNumber() < currentFrameIndex) {
                        readAheadGrabber.grab();
                    }


                    long timeAfter = System.nanoTime();
                    System.out.printf("Processed %d frames in: %d ms\n", framesToProcess, ((timeAfter - timeBefore) / 1000000));


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
