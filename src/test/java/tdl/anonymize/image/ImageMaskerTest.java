package tdl.anonymize.image;

import java.nio.file.Path;
import java.nio.file.Paths;
import static org.bytedeco.javacpp.opencv_core.CV_32FC1;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.UMat;
import static org.bytedeco.javacpp.opencv_highgui.destroyAllWindows;
import static org.bytedeco.javacpp.opencv_highgui.imshow;
import static org.bytedeco.javacpp.opencv_highgui.waitKey;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;

public class ImageMaskerTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void findSubImageAndRemoveAllOccurencesShouldSucceed() throws ImageMaskerException {
        Path mainImagePath = Paths.get("./src/test/resources/images/barcode-image.png");
        Path subImagePath = Paths.get("./src/test/resources/images/qrcode-subimage-1.png");
        ImageMasker matcher = new ImageMasker(subImagePath);
        Mat mainImage = imread(mainImagePath.toString());
        UMat mainImageAccld = mainImage.getUMat(CV_32FC1);
        matcher.mask(mainImageAccld);
        //matcher.showImage();
//        imshow("Marked", mainImage);
//        waitKey(0);
//        destroyAllWindows();
    }

}
