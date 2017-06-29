package tdl.record.image.input;

import tdl.record.utils.ImageConversion;

import java.awt.*;
import java.awt.image.BufferedImage;

public class InputFromScreen implements ImageInput {
    private static final int IMAGE_TYPE = BufferedImage.TYPE_3BYTE_BGR;
    private Rectangle screenBounds;
    private Robot robot;

    public InputFromScreen() {
        this.screenBounds = null;
        this.robot = null;
    }

    @Override
    public void open() throws InputImageGenerationException {
        try {
            //OBS: Robot requires AWTPermission, it might be wise to use a policy file, see http://docs.oracle.com/javase/7/docs/technotes/guides/security/PolicyFiles.html
            //OBS: Robot starts an app called AppMain. What is the deal with it ?
            Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
            this.robot = new Robot();
            this.screenBounds = new Rectangle(defaultToolkit.getScreenSize());
        } catch (AWTException e) {
            throw new InputImageGenerationException(e);
        }
    }

    @Override
    public BufferedImage readImage() {
        BufferedImage screenCapture = robot.createScreenCapture(screenBounds);
        return ImageConversion.convertToType(screenCapture, IMAGE_TYPE);
    }

    @Override
    public BufferedImage getSampleImage() {
        return new BufferedImage(screenBounds.width, screenBounds.height, IMAGE_TYPE);
    }

    @Override
    public int getWidth() {
        return screenBounds.width;
    }

    @Override
    public int getHeight() {
        return screenBounds.height;
    }

    @Override
    public void close() {
        //No resources to clear
    }
}
