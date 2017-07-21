package tdl.anonymize.video;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    private static final ToMat FRAME_CONVERTER = new ToMat();

    public static class Work implements AutoCloseable {

        private final Mat image;
        private final Long timestamp;

        public Work(Mat image, Long timestamp) {
            this.image = image;
            this.timestamp = timestamp;
        }

        @Override
        public void close() throws Exception {
            image.close();
        }
    }

    public static class Broker {

        private final LinkedBlockingQueue<Work> queue = new LinkedBlockingQueue();

        private Boolean isProducing = Boolean.TRUE;

        public void put(Work work) throws InterruptedException {
            this.queue.put(work);
        }

        public Work get() throws InterruptedException {
            return this.queue.poll(1, TimeUnit.SECONDS);
        }

        public Boolean isProducing() {
            return this.isProducing;
        }

        public void stop() {
            this.isProducing = Boolean.FALSE;
        }
    }

    public static class Producer implements Runnable {

        private final FrameGrabber grabber;

        private final Broker broker;

        public Producer(FrameGrabber grabber, Broker broker) throws FrameGrabber.Exception {
            this.grabber = grabber;
            this.broker = broker;
        }

        @Override
        public void run() {
            Frame frame;
            try {
                while ((frame = grabber.grab()) != null) {
                    long timestamp = grabber.getTimestamp();
                    Mat image = FRAME_CONVERTER.convert(frame);
                    Work work = new Work(image.clone(), timestamp);
                    System.out.println("Producing... " + timestamp);
                    broker.put(work);
                }
            } catch (FrameGrabber.Exception | InterruptedException ex) {
                broker.stop();
            }
        }

    }

    public static class Consumer implements Runnable {

        private final FrameRecorder recorder;

        private final Broker broker;

        public Consumer(FrameRecorder recorder, Broker broker) throws FrameRecorder.Exception {
            this.recorder = recorder;
            this.broker = broker;
        }

        @Override
        public void run() {
            try {
                Work work = broker.get();
                while (broker.isProducing() && work != null) {
                    System.out.println("Consuming... " + work.timestamp);
                    Frame edited = FRAME_CONVERTER.convert(work.image);
                    recorder.setTimestamp(work.timestamp);
                    recorder.record(edited);
                    work = broker.get();
                }
            } catch (InterruptedException | FrameRecorder.Exception ex) {
                Logger.getLogger(VideoMasker.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

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

    public void run() throws FrameGrabber.Exception, ImageMaskerException, FrameRecorder.Exception, InterruptedException, ExecutionException {
        long timeBefore = System.nanoTime();
        Broker broker = new Broker();
        ExecutorService executor = Executors.newFixedThreadPool(3);

        try (FFmpegFrameGrabber grabber = createGrabber()) {
            grabber.start();

            try (FFmpegFrameRecorder recorder = createRecorder(grabber)) {
                recorder.start();

                executor.execute(new Producer(grabber, broker));
                executor.execute(new Consumer(recorder, broker));
                //executor.execute(new Consumer(recorder, broker));

                executor.shutdown();
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            }
        }
        long timeAfter = System.nanoTime();
        System.out.printf("All framed processed in: %d ms\n", ((timeAfter - timeBefore) / 1000000));
    }

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
