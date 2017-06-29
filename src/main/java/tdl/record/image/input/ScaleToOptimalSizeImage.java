package tdl.record.image.input;

import tdl.record.utils.ImageQualityHint;
import tdl.record.utils.ImageResolution;
import tdl.record.utils.ImageResolutionPicker;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class ScaleToOptimalSizeImage implements ImageInput {
    private final ImageInput originalInputSource;
    private final ImageQualityHint imageQualityHint;
    private ImageResolution maxResolution;
    private BufferedImage targetImage;
    private AffineTransformOp scaleDownTransform;


    public ScaleToOptimalSizeImage(ImageQualityHint imageQualityHint, ImageInput originalImageSource) {
        this.originalInputSource = originalImageSource;
        this.imageQualityHint = imageQualityHint;
    }

    @Override
    public void open() throws InputImageGenerationException {
        originalInputSource.open();
        ImageResolution sourceResolution = ImageResolution.of(originalInputSource.getWidth(), originalInputSource.getHeight());
        maxResolution = sourceResolution;

        if (ImageResolutionPicker.canSupport(sourceResolution.getRatio())) {
            maxResolution = ImageResolutionPicker.maxResolutionFor(sourceResolution.getRatio(), imageQualityHint);
        }

        if (sourceResolution.getNumPixels() > maxResolution.getNumPixels()) {
            targetImage = new BufferedImage(maxResolution.getWidth(), maxResolution.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            double sx = maxResolution.getWidth() / (double) originalInputSource.getWidth();
            double sy = maxResolution.getHeight() / (double) originalInputSource.getHeight();
            AffineTransform at = new AffineTransform();
            at.scale(sx, sy);
            scaleDownTransform = new AffineTransformOp(at, AffineTransformOp.TYPE_BICUBIC);
        } else {
            targetImage = new BufferedImage(sourceResolution.getWidth(), sourceResolution.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        }
    }

    @Override
    public BufferedImage readImage() throws InputImageGenerationException {
        return processImage(originalInputSource.readImage());
    }

    @Override
    public BufferedImage getSampleImage() throws InputImageGenerationException {
        return processImage(originalInputSource.getSampleImage());
    }

    private BufferedImage processImage(BufferedImage originalImage) throws InputImageGenerationException {
        if (originalImage.getWidth() * originalImage.getHeight() > maxResolution.getNumPixels()) {
            scaleDownTransform.filter(originalImage, targetImage);
        } else {
            targetImage = originalImage;
        }

        return targetImage;
    }

    @Override
    public int getWidth() {
        return targetImage.getWidth();
    }

    @Override
    public int getHeight() {
        return targetImage.getHeight();
    }

    @Override
    public void close() {
        originalInputSource.close();
    }
}
