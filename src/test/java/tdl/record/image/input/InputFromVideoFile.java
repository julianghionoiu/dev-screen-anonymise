package tdl.record.image.input;

import tdl.record.image.output.OutputToInMemoryBuffer;
import tdl.record.time.TimeSource;
import tdl.record.video.VideoPlayer;
import tdl.record.video.VideoPlayerException;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class InputFromVideoFile implements ImageInput {
    private final String videoUrl;
    private final VideoPlayer videoPlayer;
    private final OutputToInMemoryBuffer inMemoryBuffer;
    private final TimeSource timeSource;
    private long systemStartTime;

    public InputFromVideoFile(String videoUrl, TimeSource timeSource) {
        this.videoUrl = videoUrl;
        this.inMemoryBuffer = new OutputToInMemoryBuffer();
        this.videoPlayer = new VideoPlayer(inMemoryBuffer, timeSource);
        this.timeSource = timeSource;
    }

    @Override
    public void open() throws InputImageGenerationException {
        try {
            videoPlayer.open(videoUrl);
            systemStartTime = timeSource.currentTimeNano();
        } catch (VideoPlayerException e) {
            throw new InputImageGenerationException(e);
        }
    }

    @Override
    public BufferedImage readImage() throws InputImageGenerationException {
        try {
            long currentTime = timeSource.currentTimeNano();
            videoPlayer.seekTo(currentTime - systemStartTime, TimeUnit.NANOSECONDS);
            return inMemoryBuffer.getCurrentImage();
        } catch (IOException | InterruptedException e) {
            throw new InputImageGenerationException(e);
        }
    }

    @Override
    public BufferedImage getSampleImage() {
        return inMemoryBuffer.getSuggestedOutputSample(getWidth(), getHeight());
    }

    @Override
    public int getWidth() {
        return videoPlayer.getWidth();
    }

    @Override
    public int getHeight() {
        return videoPlayer.getHeight();
    }

    @Override
    public void close()  {
        videoPlayer.close();
    }
}
