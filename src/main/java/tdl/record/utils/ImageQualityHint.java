package tdl.record.utils;

public enum ImageQualityHint {
    LOW(1024000),
    MEDIUM(1296000),
    HIGH(1764000);

    private int maxNumPixels;

    ImageQualityHint(int maxNumPixels) {
        this.maxNumPixels = maxNumPixels;
    }

    public int getMaxNumPixels() {
        return maxNumPixels;
    }
}
