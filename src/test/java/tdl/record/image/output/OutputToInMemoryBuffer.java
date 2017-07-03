package tdl.record.image.output;

import java.awt.image.BufferedImage;

public class OutputToInMemoryBuffer implements ImageOutput {
    private BufferedImage currentImage;

    public OutputToInMemoryBuffer() {
        currentImage = null;
    }

    @Override
    public void open() {
        currentImage = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
    }

    @Override
    public BufferedImage getSuggestedOutputSample(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    }

    @Override
    public void writeImage(BufferedImage image) {
        currentImage = image;
    }

    public BufferedImage getCurrentImage() {
        return currentImage;
    }

    @Override
    public void close() {
        //Nothing to close
    }
}
