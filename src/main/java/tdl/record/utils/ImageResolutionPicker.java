package tdl.record.utils;

import java.util.*;

public class ImageResolutionPicker {

    enum Resolutions {
        R4_3(4 / 3D, from(
                "640x480", "800x600", "960x720", "1024x768", "1280x960",
                "1400x1050", "1440x1080", "1600x1200", "1856x1392", "1920x1440", "2048x1536")),
        R16_10(16 / 10D, from(
                "1280x800", "1440x900", "1680x1050", "1920x1200", "2560x1600")),
        R16_9(16 / 9D, from(
                "1024x576", "1152x648", "1280x720", "1366x768", "1600x900", "1920x1080", "2560x1440", "3840x2160"));

        public static Optional<Resolutions> forRatio(double ratio) {
            return Arrays.stream(Resolutions.values())
                    .filter(resolutions -> Math.abs(resolutions.ratio - ratio) < 0.1)
                    .findFirst();
        }

        private double ratio;
        private List<ImageResolution> resolutions;

        Resolutions(double ratio, List<ImageResolution> resolutions) {
            this.ratio = ratio;
            this.resolutions = resolutions;
        }

        public List<ImageResolution> getResolutions() {
            return resolutions;
        }
    }

    public static boolean canSupport(double ratio) {
        return Resolutions.forRatio(ratio).isPresent();
    }

    public static ImageResolution maxResolutionFor(double ratio, ImageQualityHint imageQualityHint) {
        List<ImageResolution> eligibleResolutions = Resolutions.forRatio(ratio)
                .map(Resolutions::getResolutions)
                .orElseThrow(() -> new IllegalArgumentException("Resolution not supported"));

        return eligibleResolutions.stream()
                .filter(imageResolution -> imageResolution.getNumPixels() <= imageQualityHint.getMaxNumPixels())
                .sorted(Comparator.comparing(ImageResolution::getNumPixels).reversed())
                .findFirst().orElseThrow(() -> new IllegalArgumentException("The quality hints are not balanced correctly"));
    }

    private static List<ImageResolution> from(String... resolutions) {
        List<ImageResolution> resolutionList = new ArrayList<>();
        for (String resolution : resolutions) {
            resolutionList.add(ImageResolution.of(resolution));
        }
        return resolutionList;
    }
}
