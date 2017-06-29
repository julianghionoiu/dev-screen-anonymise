package tdl.record.image.input;

import java.awt.image.BufferedImage;
import java.io.IOException;

public interface ImageInput {

    void open() throws InputImageGenerationException;

    BufferedImage readImage()
            throws InputImageGenerationException;

    /**
     * Provide a sample of the input. This is required to allow the client to initialize the converters ahead of time
     *
     * @return A sample build with the format provided by the input
     * @throws InputImageGenerationException it there was a problem creating the sample image
     */
    BufferedImage getSampleImage() throws InputImageGenerationException;

    int getWidth();

    int getHeight();

    void close();
}
