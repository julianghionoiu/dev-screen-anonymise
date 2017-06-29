package tdl.record.image.input;

import java.awt.image.BufferedImage;

public class InputFromErrorProneSource implements ImageInput {
    private final ImageInput wrappedImageInput;
    private int countToError;

    public InputFromErrorProneSource(int countToError, ImageInput wrappedImageInput) {
        this.wrappedImageInput = wrappedImageInput;
        this.countToError = countToError;
    }

    @Override
    public void open() throws InputImageGenerationException {
        wrappedImageInput.open();
    }

    @Override
    public BufferedImage readImage() throws InputImageGenerationException {
        if (countToError == 0) {
            throw new IllegalStateException("The image source crashed");
        }

        countToError--;
        return wrappedImageInput.readImage();
    }

    @Override
    public BufferedImage getSampleImage() throws InputImageGenerationException {
        return wrappedImageInput.getSampleImage();
    }

    @Override
    public int getWidth() {
        return wrappedImageInput.getWidth();
    }

    @Override
    public int getHeight() {
        return wrappedImageInput.getHeight();
    }

    @Override
    public void close() {
        wrappedImageInput.close();
    }
}
