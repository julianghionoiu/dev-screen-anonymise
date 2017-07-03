package tdl.record.image.input;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class InputFromStaticImage implements ImageInput {
    private final String pathname;
    private BufferedImage staticImage;

    public InputFromStaticImage(String pathname) {
        this.pathname = pathname;
    }

    @Override
    public void open() throws InputImageGenerationException {
        try {
            staticImage = ImageIO.read(new File(pathname));
        } catch (IOException e) {
            throw new InputImageGenerationException(e);
        }
    }

    @Override
    public BufferedImage readImage() throws InputImageGenerationException {
        return staticImage;
    }

    @Override
    public BufferedImage getSampleImage() throws InputImageGenerationException {
        return staticImage;
    }

    @Override
    public int getWidth() {
        return staticImage.getWidth();
    }

    @Override
    public int getHeight() {
        return staticImage.getHeight();
    }

    @Override
    public void close() {

    }
}
