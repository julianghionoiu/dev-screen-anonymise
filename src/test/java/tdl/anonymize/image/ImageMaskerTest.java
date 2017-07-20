package tdl.anonymize.image;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import tdl.anonymize.image.ImageMaskerException;

public class ImageMaskerTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void findSubImageAndRemoveAllOccurencesShouldSucceed() throws ImageMaskerException {
        Path mainImagePath = Paths.get("./src/test/resources/images/barcode-image.png");
        Path subImagePath = Paths.get("./src/test/resources/images/qrcode-subimage-1.png");
        ImageMasker matcher = new ImageMasker(mainImagePath);
        matcher.removeAllOccurences(subImagePath);
        //matcher.showImage();
    }
}
