package tdl.anonymize.image;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.indexer.IntRawIndexer;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_imgproc;
import static org.bytedeco.javacpp.opencv_core.CV_32FC1;
import static org.bytedeco.javacpp.opencv_core.CV_8UC1;
import static org.bytedeco.javacpp.opencv_core.minMaxLoc;
import static org.bytedeco.javacpp.opencv_core.findNonZero;
import static org.bytedeco.javacpp.opencv_highgui.destroyAllWindows;
import static org.bytedeco.javacpp.opencv_highgui.imshow;
import static org.bytedeco.javacpp.opencv_highgui.waitKey;
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

    private int counter = 0;

    public ImageMasker(Path subImagePath) throws ImageMaskerException {
        this(imread(subImagePath.toString()));
    }

    public ImageMasker(Mat subImage) throws ImageMaskerException {
        this.subImage = subImage;
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
        counter = 0;
        try (Mat mainImageGrey = createGreyImage(mainImage)) {

            Size size = new Size(
                    mainImageGrey.cols() - subImageGrey.cols() + 1,
                    mainImageGrey.rows() - subImageGrey.rows() + 1
            );

            try (Mat result = new Mat(size, CV_32FC1);
                    Mat result2 = new Mat(size, CV_8UC1)) {
                matchTemplate(mainImageGrey, subImageGrey, result, TM_CCOEFF_NORMED);

                result.convertTo(result2, CV_8UC1);

                threshold(result2, result2, THRESHOLD, 1, THRESH_TOZERO);

                Mat locationMat = new Mat();
                findNonZero(result2, locationMat);
                List<Point> similarPoints = collectSimilarPointsFromMat(locationMat);
                int maxDistance = Math.min(subImage.cols(), subImage.rows());
                List<Point> clusteredPoints = clusterPoints(similarPoints, maxDistance);

                clusteredPoints.stream().forEach((point) -> {
                    Rect rect = new Rect(point.x(), point.y(), subImage.cols(), subImage.rows());
                    blurImage(mainImage, rect);
                    rectangle(result, rect, new Scalar(0, 0, 0, 0));
                });
            }
        }
    }

    public static List<Point> collectSimilarPointsFromMat(Mat mat) {
        IntRawIndexer indexer = mat.createIndexer();
        List<Point> list = new ArrayList<>();
        for (int y = 0; y < mat.rows(); y++) {
            for (int x = 0; x < mat.cols(); x++) {
                int pointX = indexer.get(x, y, 0);
                int pointY = indexer.get(x, y, 1);
                list.add(new Point(pointX, pointY));
            }
        }
        return list;
    }

    public static List<Point> clusterPoints(List<Point> list, int maxDistance) {
        Point previous = list.get(0);
        List<Point> clustered = new ArrayList<>();
        clustered.add(previous);
        for (Point point : list) {
            int distance = euclideanDistance(previous, point);
            if (distance > maxDistance) {
                clustered.add(point);
            }
            previous = point;
        }
        return clustered;
    }

    public static int euclideanDistance(Point p1, Point p2) {
        return (new Double(Math.sqrt(Math.pow(p1.x() - p2.x(), 2) + Math.pow(p1.y() - p2.y(), 2)))).intValue();
    }

    public static List<Integer> clusterIntegers(List<Integer> numbers, int maxRange) {
        Integer previous = 0;
        List<Integer> averages = new ArrayList<>();
        List<Integer> currentList = new ArrayList<>();
        for (Integer number : numbers) {
            if (number - previous > maxRange) {
                Integer average = average(currentList);
                averages.add(average);
                currentList.clear();
            }
            currentList.add(number);
            previous = number;
        }
        Integer average = average(currentList);
        averages.add(average);
        return averages.stream().filter(i -> i > 0).collect(Collectors.toList());
    }

    public static Integer average(List<Integer> numbers) {
        if (numbers.isEmpty()) {
            return 0;
        }
        Integer sum = numbers.stream().mapToInt(Integer::intValue).sum();
        return (new Double(Math.ceil((double) sum / numbers.size()))).intValue();
    }

    private static void blurImage(Mat searchImage, Rect rect) {
        int kernelWidth = (int) Math.round(rect.size().width() / 2);
        int kernelHeight = (int) Math.round(rect.size().height() / 2);
        try (Mat region = new Mat(searchImage, rect);
                Size kernelSize = new Size(kernelWidth, kernelHeight)) {
            opencv_imgproc.blur(region, region, kernelSize);
        }
    }

    public Mat getImage() {
        return subImage;
    }

    public int getCount() {
        return counter;
    }

    @Override
    public void close() throws Exception {
        subImage.close();
        subImageGrey.close();
    }
}
