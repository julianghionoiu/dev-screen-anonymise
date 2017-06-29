package tdl.record.image.input;

import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

public class InputFromFreezingSource implements ImageInput {
    private final ImageInput wrappedImageInput;
    private int countToFreeze;

    public InputFromFreezingSource(int countToFreeze, ImageInput wrappedImageInput) {
        this.wrappedImageInput = wrappedImageInput;
        this.countToFreeze = countToFreeze;
    }

    @Override
    public void open() throws InputImageGenerationException {
        wrappedImageInput.open();
    }

    @Override
    public BufferedImage readImage() throws InputImageGenerationException {
        if (countToFreeze == 0) {
            try {
                Thread.sleep(TimeUnit.MINUTES.toMillis(90));
            } catch (InterruptedException e) {
                throw new InputImageGenerationException(e);
            }
        }

        countToFreeze--;
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
