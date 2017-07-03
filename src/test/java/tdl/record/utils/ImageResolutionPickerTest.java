package tdl.record.utils;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

public class ImageResolutionPickerTest {


    @Test
    public void should_honour_quality_hints() throws Exception {
        double ratio = 4 / 3D;

        ImageResolution low = ImageResolutionPicker.maxResolutionFor(ratio, ImageQualityHint.LOW);
        ImageResolution medium = ImageResolutionPicker.maxResolutionFor(ratio, ImageQualityHint.MEDIUM);

        assertThat(low.getNumPixels(), lessThan(medium.getNumPixels()));
    }

    @Test
    public void should_honour_ratios() throws Exception {
        ImageQualityHint hint = ImageQualityHint.MEDIUM;
        ImageResolution image = ImageResolutionPicker.maxResolutionFor(4 / 3D, hint);

        assertThat(image.getRatio(), closeTo(4 /3D, 0.1));
    }

    @Test
    public void should_only_work_for_supported_ratios() throws Exception {
        assertThat(ImageResolutionPicker.canSupport(1/1D), is(false));
        assertThat(ImageResolutionPicker.canSupport(4/3D), is(true));
    }

}