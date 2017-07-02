package tdl.anonymize.image;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.bytedeco.javacpp.DoublePointer;
import static org.bytedeco.javacpp.opencv_core.CV_32FC1;
import static org.bytedeco.javacpp.opencv_core.CV_8UC1;
import org.bytedeco.javacpp.opencv_core.CvRect;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Rect;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;
import static org.bytedeco.javacpp.opencv_core.cvCopy;
import static org.bytedeco.javacpp.opencv_core.cvResetImageROI;
import static org.bytedeco.javacpp.opencv_core.cvSetImageROI;
import static org.bytedeco.javacpp.opencv_core.minMaxLoc;
import static org.bytedeco.javacpp.opencv_highgui.destroyAllWindows;
import static org.bytedeco.javacpp.opencv_highgui.imshow;
import static org.bytedeco.javacpp.opencv_highgui.waitKey;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;
import static org.bytedeco.javacpp.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_FILLED;
import static org.bytedeco.javacpp.opencv_imgproc.TM_CCORR_NORMED;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.matchTemplate;
import static org.bytedeco.javacpp.opencv_imgproc.rectangle;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

public class ImageMasker {

    private static final double THRESHOLD = 0.99;

    private final Mat image;

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

    public Rect findSubImagePosition(Path subImagePath) throws ImageMaskerException {
        Mat subImage = imread(subImagePath.toString());
        if (subImage.empty()) {
            throw new ImageMaskerException("Cannot open image " + subImagePath.getFileName());
        }
        return findSubImagePositionInMainImage(image, subImage);
    }

    public static Rect findSubImagePositionInMainImage(Mat mainImage, Mat subImage) throws ImageMaskerException {
        Mat mainImageGrey = new Mat(mainImage.size(), CV_8UC1);
        Mat subImageGrey = new Mat(subImage.size(), CV_8UC1);
        cvtColor(mainImage, mainImageGrey, COLOR_BGR2GRAY);
        cvtColor(subImage, subImageGrey, COLOR_BGR2GRAY);
        Size size = new Size(mainImageGrey.cols() - subImageGrey.cols() + 1, mainImageGrey.rows() - subImageGrey.rows() + 1);
        Mat result = new Mat(size, CV_32FC1);
        matchTemplate(mainImageGrey, subImageGrey, result, TM_CCORR_NORMED);
        DoublePointer minVal = new DoublePointer(1);
        DoublePointer maxVal = new DoublePointer(1);
        Point min = new Point();
        Point max = new Point();
        minMaxLoc(result, minVal, maxVal, min, max, null);
        double val = maxVal.get();
        if (val < THRESHOLD) {
            throw new ImageMaskerException("Cannot find sub image");
        }
        return new Rect(max.x(), max.y(), subImageGrey.cols(), subImageGrey.rows());
    }

    public List<Rect> findAllSubImagePositions(Path subImagePath) throws ImageMaskerException {
        Mat subImage = imread(subImagePath.toString());
        List<Rect> list = new ArrayList<>();
        Mat searchImage = new Mat(image);
        while (true) {
            try {
                Rect rect = findSubImagePositionInMainImage(searchImage, subImage);
                removeRegionFromImage(searchImage, rect);
                list.add(rect);
            } catch (ImageMaskerException e) {
                break;
            }
        }
        if (list.isEmpty()) {
            throw new ImageMaskerException("Cannot find any occurences");
        }
        return list;
    }

    private static void removeRegionFromImage(Mat image, Rect rect) {
        Scalar randomColor = new Scalar(255, 0, 0, 0);
        rectangle(image, rect, randomColor, CV_FILLED, 0, 0);
    }

    public void findSubImageAndRemoveAllOccurences(Path subImagePath) throws ImageMaskerException {
        List<Rect> list = findAllSubImagePositions(subImagePath);
        //Need to work using OpenCV native function.
        IplImage mainImageIpl = new IplImage(image);

        //TODO: Change into pixelization
        list.stream().forEach((rect) -> {
            Mat stamp = getResizedStamp(rect);
            CvRect cvRect = new CvRect(rect.x(), rect.y(), rect.width(), rect.height());

            cvSetImageROI(mainImageIpl, cvRect);
            cvCopy(new IplImage(stamp), mainImageIpl);
            cvResetImageROI(mainImageIpl);
        });
        Mat removed = new Mat(mainImageIpl);
        removed.copyTo(image);
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
}
