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
    
    private final UMat subImageAccld;

    private final UMat subImageGrey;

    private int counter = 0;

    public ImageMasker(Path subImagePath) throws ImageMaskerException {
        this(imread(subImagePath.toString()));
    }

    public ImageMasker(Mat subImage) throws ImageMaskerException {
        this.subImage = subImage;
        throwExceptionIfMainImageIsEmpty();
        this.subImageAccld = subImage.getUMat(CV_32FC1);
        this.subImageGrey = createGreyImage(subImageAccld);
    }

    private void throwExceptionIfMainImageIsEmpty() throws ImageMaskerException {
        if (subImage.empty()) {
            throw new ImageMaskerException("Cannot open image");
        }
    }

    private UMat createGreyImage(UMat image) {
        UMat grey = new UMat(image.size(), CV_8UC1);
        cvtColor(image, grey, COLOR_BGR2GRAY);
        return grey;
    }

    public void mask(UMat mainImage) {
        counter = 0;
        try (UMat mainImageGrey = createGreyImage(mainImage)) {

            Size size = new Size(
                    mainImageGrey.cols() - subImageGrey.cols() + 1,
                    mainImageGrey.rows() - subImageGrey.rows() + 1
            );

            try (UMat match_result = new UMat(size, CV_32FC1);
                    UMat threshold_result = new UMat(size, CV_8UC1)) {

                matchTemplate(mainImageGrey, subImageGrey, match_result, TM_CCOEFF_NORMED);
//                imwrite("build/after_match.png", multiply(match_result, 255).asMat());

                threshold(match_result, match_result, THRESHOLD, 1, THRESH_TOZERO);
//                imwrite("build/"+frameIndex+"_after_threshold.png", multiply(match_result, 255).asMat());

                match_result.convertTo(threshold_result, CV_8UC1);

                UMat locationMat = new UMat();
                findNonZero(threshold_result, locationMat);
                List<Point> similarPoints = collectSimilarPointsFromMat(locationMat);
                if (similarPoints.isEmpty()) {
                    return;
                }
                
                int maxDistance = Math.min(subImage.cols(), subImage.rows());
                List<Point> clusteredPoints = clusterPoints(similarPoints, maxDistance);
                clusteredPoints.stream().forEach((point) -> {
                    Rect rect = new Rect(point.x(), point.y(), subImage.cols(), subImage.rows());
                    blurImage(mainImage, rect);
                    Point point2 = new Point(point.x() + subImage.cols(), point.y() + subImage.rows());
                    rectangle(match_result, point, point2, AbstractScalar.BLACK);
                });
            }
        }
    }

    public static List<Point> collectSimilarPointsFromMat(UMat mat) {
        Mat toIndexMat = mat.getMat(CV_8UC1);
        List<Point> list = new ArrayList<>();
        try {
            IntRawIndexer indexer = toIndexMat.createIndexer();
            for (int y = 0; y < mat.rows(); y++) {
                for (int x = 0; x < mat.cols(); x++) {
                    int pointX = indexer.get(x, y, 0);
                    int pointY = indexer.get(x, y, 1);
                    list.add(new Point(pointX, pointY));
                }
            }
        } catch (NullPointerException ex) {
            //Do nothing
        } finally {
            return list;
        }
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

    private static void blurImage(UMat searchImage, Rect rect) {
        int kernelWidth = (int) Math.round(rect.size().width() / 2);
        int kernelHeight = (int) Math.round(rect.size().height() / 2);
        try (UMat region = new UMat(searchImage, rect);
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
