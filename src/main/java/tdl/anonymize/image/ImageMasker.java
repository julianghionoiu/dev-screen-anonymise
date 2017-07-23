package tdl.anonymize.image;

import org.bytedeco.javacpp.indexer.IntRawIndexer;
import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.opencv_imgproc;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgproc.*;

public class ImageMasker implements AutoCloseable {

    private static final double THRESHOLD = 0.99;

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

            try (Mat match_result = new Mat(size, CV_32FC1);
                    Mat threshold_result = new Mat(size, CV_8UC1)) {

                matchTemplate(mainImageGrey, subImageGrey, match_result, TM_CCOEFF_NORMED);
//                imwrite("build/after_match.png", multiply(match_result, 255).asMat());

                threshold(match_result, match_result, THRESHOLD, 1, THRESH_TOZERO);
//                imwrite("build/"+frameIndex+"_after_threshold.png", multiply(match_result, 255).asMat());

                match_result.convertTo(threshold_result, CV_8UC1);

                Mat locationMat = new Mat();
                findNonZero(threshold_result, locationMat);
                List<Point> similarPoints = collectSimilarPointsFromMat(locationMat);
                if (similarPoints.isEmpty()) {
                    return;
                }

                int maxDistance = Math.min(subImage.cols(), subImage.rows());
                List<Point> clusteredPoints = clusterPoints(similarPoints, maxDistance);
                clusteredPoints.forEach((point) -> {
                    Rect rect = new Rect(point.x(), point.y(), subImage.cols(), subImage.rows());
                    blurImage(mainImage, rect);
                    rectangle(match_result, rect, new Scalar(0, 0, 0, 0));
                });
            }
        }
    }

    private static List<Point> collectSimilarPointsFromMat(Mat mat) {
        List<Point> list = new ArrayList<>();
        try {
            IntRawIndexer indexer = mat.createIndexer();
            for (int y = 0; y < mat.rows(); y++) {
                for (int x = 0; x < mat.cols(); x++) {
                    int pointX = indexer.get(x, y, 0);
                    int pointY = indexer.get(x, y, 1);
                    list.add(new Point(pointX, pointY));
                }
            }
        } catch (NullPointerException ex) {
            //Do nothing
        }

        return list;
    }

    private static List<Point> clusterPoints(List<Point> list, int maxDistance) {
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

    private static int euclideanDistance(Point p1, Point p2) {
        return (new Double(Math.sqrt(Math.pow(p1.x() - p2.x(), 2) + Math.pow(p1.y() - p2.y(), 2)))).intValue();
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
