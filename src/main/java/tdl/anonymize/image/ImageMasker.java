package tdl.anonymize.image;

import java.nio.file.Path;
import java.util.ArrayList;
import org.bytedeco.javacpp.DoublePointer;
import static org.bytedeco.javacpp.opencv_core.CV_32FC1;
import static org.bytedeco.javacpp.opencv_core.CV_8UC1;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;
import static org.bytedeco.javacpp.opencv_core.minMaxLoc;
import static org.bytedeco.javacpp.opencv_highgui.destroyAllWindows;
import static org.bytedeco.javacpp.opencv_highgui.imshow;
import static org.bytedeco.javacpp.opencv_highgui.waitKey;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;
import org.bytedeco.javacpp.opencv_imgproc;
import static org.bytedeco.javacpp.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.THRESH_TOZERO;
import static org.bytedeco.javacpp.opencv_imgproc.TM_CCOEFF_NORMED;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.matchTemplate;
import static org.bytedeco.javacpp.opencv_imgproc.rectangle;
import static org.bytedeco.javacpp.opencv_imgproc.resize;
import static org.bytedeco.javacpp.opencv_imgproc.threshold;

public class ImageMasker implements AutoCloseable {

    private static final double THRESHOLD = 0.96;

    private final Mat image;

    private int counter = 0;

    public ImageMasker(Path imagePath) throws ImageMaskerException {
        image = imread(imagePath.toString());
        throwExceptionIfMainImageIsEmpty();
    }

    public ImageMasker(Mat image) throws ImageMaskerException {
        this.image = image;
        throwExceptionIfMainImageIsEmpty();
    }

    private final void throwExceptionIfMainImageIsEmpty() throws ImageMaskerException {
        if (image.empty()) {
            throw new ImageMaskerException("Cannot open image");
        }
    }

    public int getCount() {
        return counter;
    }

    public void removeAllOccurences(Path subImagePath) throws ImageMaskerException {
        try (
                Mat subImage = imread(subImagePath.toString());
                Mat searchImage = new Mat(image);
                Mat mainImageGrey = new Mat(searchImage.size(), CV_8UC1);
                Mat subImageGrey = new Mat(subImage.size(), CV_8UC1)) {

            cvtColor(searchImage, mainImageGrey, COLOR_BGR2GRAY);
            cvtColor(subImage, subImageGrey, COLOR_BGR2GRAY);
            Size size = new Size(mainImageGrey.cols() - subImageGrey.cols() + 1, mainImageGrey.rows() - subImageGrey.rows() + 1);

            try (Mat result = new Mat(size, CV_32FC1)) {
                matchTemplate(mainImageGrey, subImageGrey, result, TM_CCOEFF_NORMED);
                threshold(result, result, 0.1, 1, THRESH_TOZERO);

                removeAllMatchTemplateResult(result, subImage, searchImage);
            }
        }
    }

    private void removeAllMatchTemplateResult(Mat result, Mat subImage, Mat searchImage) {
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

                int kernelWidth = (int) Math.round(rect.size().width() / 2);
                int kernelHeight = (int) Math.round(rect.size().height() / 2);
                try (Mat region = new Mat(searchImage, rect);
                        Size kernelSize = new Size(kernelWidth, kernelHeight)) {
                    opencv_imgproc.blur(region, region, kernelSize);
                }
                rectangle(result, rect, new Scalar(0, 0, 0, 0));
            }
        }
    }

    public Mat getImage() {
        return image;
    }

    public void showImage() {
        //For debugging.
        imshow("Marked", new Mat(image));
        waitKey(0);
        destroyAllWindows();
    }

    public void saveImage(Path path) {
        imwrite(path.toString(), image);
    }

    public static Mat getResizedStamp(Rect rect) {
        //TODO: Read from resource
        Mat stamp = imread("src/main/resources/pixelated_stamp.jpg");
        resize(stamp, stamp, rect.size());
        return stamp;
    }

    @Override
    public void close() throws Exception {
        image.close();
    }
}
