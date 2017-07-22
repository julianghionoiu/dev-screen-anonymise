package tdl.anonymize.image;

import java.nio.file.Path;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_imgproc;
import static org.bytedeco.javacpp.opencv_core.CV_32FC1;
import static org.bytedeco.javacpp.opencv_core.CV_8UC1;
import static org.bytedeco.javacpp.opencv_core.minMaxLoc;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.THRESH_TOZERO;
import static org.bytedeco.javacpp.opencv_imgproc.TM_CCOEFF_NORMED;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.matchTemplate;
import static org.bytedeco.javacpp.opencv_imgproc.rectangle;
import static org.bytedeco.javacpp.opencv_imgproc.threshold;

public class ImageMasker implements AutoCloseable {

    private static final double THRESHOLD = 0.96;

    private final Mat subImage;

    private final Mat subImageGrey;

    public ImageMasker(Path subImagePath) throws ImageMaskerException {
        this.subImage = imread(subImagePath.toString());
        throwExceptionIfMainImageIsEmpty();
        this.subImageGrey = createGreyImage(subImage);
    }

    private void throwExceptionIfMainImageIsEmpty() throws ImageMaskerException {
        if (subImage.empty()) {
            throw new ImageMaskerException("Cannot open image");
        }
    }

    private Mat createGreyImage(Mat image) {
        Mat grey = new Mat(image.size(), CV_8UC1);
        cvtColor(image, grey, COLOR_BGR2GRAY);
        return grey;
    }

    public void mask(Mat mainImage) {
        try (Mat mainImageGrey = createGreyImage(mainImage)) {

            Size size = new Size(
                    mainImageGrey.cols() - subImageGrey.cols() + 1,
                    mainImageGrey.rows() - subImageGrey.rows() + 1
            );

            try (Mat result = new Mat(size, CV_32FC1)) {
                matchTemplate(mainImageGrey, subImageGrey, result, TM_CCOEFF_NORMED);
                threshold(result, result, 0.1, 1, THRESH_TOZERO);
                removeAllMatchTemplateResult(result, subImage, mainImage);
            }
        }
    }

    private void removeAllMatchTemplateResult(Mat result, Mat subImage, Mat searchImage) {
        int count = 0;
        try (
                DoublePointer minVal = new DoublePointer(1);
                DoublePointer maxVal = new DoublePointer(1);
                Point min = new Point();
                Point max = new Point()) {

            while (true) {
                minMaxLoc(result, minVal, maxVal, min, max, null);
                double val = maxVal.get();
                if (val < THRESHOLD) {
                    break;
                }
                Rect rect = new Rect(max.x(), max.y(), subImage.cols(), subImage.rows());
                blurImage(searchImage, rect);
                rectangle(result, rect, new Scalar(0, 0, 0, 0));
                count++;
            }
        }
    }

    private static void blurImage(Mat searchImage, Rect rect) {
        int kernelWidth = Math.round(rect.size().width() / 2);
        int kernelHeight = Math.round(rect.size().height() / 2);
        try (Mat region = new Mat(searchImage, rect);
                Size kernelSize = new Size(kernelWidth, kernelHeight)) {
            opencv_imgproc.blur(region, region, kernelSize);
        }
    }

    public Mat getImage() {
        return subImage;
    }

    @Override
    public void close() throws Exception {
        subImage.close();
        subImageGrey.close();
    }
}
