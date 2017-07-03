package tdl.record.utils;

public class ImageResolution {
    private final int width;
    private final int height;

    private ImageResolution(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public static ImageResolution of(int width, int height) {
        return new ImageResolution(width, height);
    }

    static ImageResolution of(String resolution) {
        String[] split = resolution.split("x");
        int width = Integer.parseInt(split[0]);
        int height = Integer.parseInt(split[1]);
        return ImageResolution.of(width, height);
    }

    public int getNumPixels() {
        return width * height;
    }

    public double getRatio() {
        return width / (double) height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public String toString() {
        return "ImageResolution{" +
                "width=" + width +
                ", height=" + height +
                '}';
    }
}
