package tdl.record.image.output;

import io.humble.video.awt.ImageFrame;

import java.awt.image.BufferedImage;

public class OutputToScreen implements ImageOutput {
    private static final int IMAGE_TYPE = BufferedImage.TYPE_3BYTE_BGR;

    private ImageFrame window;

    public OutputToScreen() {
        this.window = null;
    }

    @Override
    public void open() throws ImageOutputException {
        window = ImageFrame.make();
        if (window == null) {
            throw new ImageOutputException("Cannot create display frame. Maybe there is no screen available.");
        }
    }

    @Override
    public BufferedImage getSuggestedOutputSample(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    }

    @Override
    public void writeImage(BufferedImage image) {
        window.setImage(image);
    }

    @Override
    public void close() {
        window.dispose();
    }
}
