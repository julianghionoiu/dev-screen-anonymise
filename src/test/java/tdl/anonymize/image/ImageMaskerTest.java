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
        Path mainImagePath = Paths.get("./src/test/resources/images/main-image-2.jpg");
        Path subImagePath = Paths.get("./src/test/resources/images/subimage-1.jpg");
        ImageMasker matcher = new ImageMasker(mainImagePath);
        matcher.findSubImageAndRemoveAllOccurences(subImagePath);
        //matcher.showImage();
    }

    @Test
    public void findSubImagePositionShouldThrowExceptionIfImageNotFound() throws ImageMaskerException {
        expectedException.expect(ImageMaskerException.class);
        Path mainImagePath = Paths.get("./src/test/resources/images/main-image-2.jpg");
        Path subImagePath = Paths.get("./src/test/resources/images/subimage-1-larger.jpg");
        ImageMasker matcher = new ImageMasker(mainImagePath);
        matcher.findSubImagePosition(subImagePath);
    }
}
