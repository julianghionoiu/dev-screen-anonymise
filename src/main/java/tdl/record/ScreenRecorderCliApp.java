package tdl.record;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import lombok.extern.slf4j.Slf4j;
import tdl.record.image.input.InputFromScreen;
import tdl.record.image.input.ScaleToOptimalSizeImage;
import tdl.record.metrics.RecordingMetricsCollector;
import tdl.record.utils.ImageQualityHint;
import tdl.record.video.VideoRecorder;
import tdl.record.video.VideoRecorderException;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
public class ScreenRecorderCliApp {
    @Parameter(names = {"-o", "--output"}, description = "The path to the recording file")
    private String destinationPath = "./recording.mp4";

    @Parameter(names = {"-d", "--duration"}, description = "Duration of the recording in minutes. Pass -1 for continuous recording.")
    private Integer recordingTime = -1;


    public static void main(String[] args) throws VideoRecorderException {
        log.info("Starting recording app");
        ScreenRecorderCliApp main = new ScreenRecorderCliApp();
        new JCommander(main, args);
        main.run();
    }

    private void run() throws VideoRecorderException {
        if (recordingTime < 0) {
            throw new IllegalArgumentException("Continuous recording not implemented");
        }

        RecordingMetricsCollector recordingMetricsCollector = new RecordingMetricsCollector();
        VideoRecorder videoRecorder = new VideoRecorder
                .Builder(new ScaleToOptimalSizeImage(ImageQualityHint.MEDIUM, new InputFromScreen()))
                .withRecordingListener(recordingMetricsCollector)
                .build();

        //Issue performance updates
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Recorded "+recordingMetricsCollector.getTotalFrames() + " frames"
                        +" at "+ recordingMetricsCollector.getVideoFrameRate().getDenominator() + " fps"
                        +" with a load of " + recordingMetricsCollector.getRenderingTimeRatio());
            }
        }, 0, 5000);

        registerShutdownHook(videoRecorder, timer);


        videoRecorder.open(destinationPath, 4, 4);
        videoRecorder.start(Duration.of(recordingTime, ChronoUnit.MINUTES));
        videoRecorder.close();
        timer.cancel();
    }


    private void registerShutdownHook(final VideoRecorder videoRecorder, Timer timer) {
        final Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            timer.cancel();
            videoRecorder.stop();
            try {
                mainThread.join();
            } catch (InterruptedException e) {
                log.warn("Could not join main thread", e);
            }
        }));
    }
}
