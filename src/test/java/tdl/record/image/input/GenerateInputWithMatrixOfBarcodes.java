package tdl.record.image.input;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import tdl.record.time.FakeTimeSource;
import tdl.record.time.TimeSource;
import tdl.record.video.VideoRecorder;
import tdl.record.video.VideoRecorderException;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GenerateInputWithMatrixOfBarcodes implements ImageInput {
    public static final String BARCODE_VIDEO_PATH = "src/test/resources/rec_barcode_matrix/recording_from_matrix_of_barcodes_at_4x.mp4";
    public static final String BARCODE_WITH_STATIC_PATH = "src/test/resources/rec_barcode_static/recording_from_matrix_of_barcodes_with_static.mp4";

    private final int width;
    private final int height;
    private final BarcodeImageRaster barcodeImageRaster;
    private final TimeSource timeSource;
    private long systemStartTime;

    private GenerateInputWithMatrixOfBarcodes(int width, int height, TimeSource timeSource) {
        this.width = width;
        this.height = height;
        this.barcodeImageRaster = new BarcodeImageRaster(width, height);
        this.timeSource = timeSource;

    }

    @Override
    public void open() {
        systemStartTime = timeSource.currentTimeNano();
    }

    @Override
    public BufferedImage readImage() throws InputImageGenerationException {
        try {
            long currentTime = timeSource.currentTimeNano();
            long relativeSystemTime = currentTime - systemStartTime;
            return renderAsBarcodeMatrix(relativeSystemTime, TimeUnit.NANOSECONDS);
        } catch (BarcodeGenerationException e) {
            throw new InputImageGenerationException(e);
        }
    }

    @Override
    public BufferedImage getSampleImage() throws InputImageGenerationException {
        return new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void close() {

    }

    //~~~~ Barcode related logic

    private BufferedImage renderAsBarcodeMatrix(long timestamp, TimeUnit timeUnit) throws BarcodeGenerationException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        BitMatrix barcode2D;
        BitMatrix barcodeQR;

        try {
            long seconds = timeUnit.toSeconds(timestamp);
            long millis = timeUnit.toMillis(timestamp) - TimeUnit.SECONDS.toMillis(seconds);

            String barCodeContents = Long.toString(timeUnit.toMillis(timestamp));
            String label = String.format("%d.%03d", seconds, millis);
            if (seconds == 0 && millis >= 400 && millis <= 800) {
                barCodeContents = "999";
                label = "static";
            }


            MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
            barcode2D = multiFormatWriter.encode(
                    barCodeContents, BarcodeFormat.CODE_39,
                    barcodeImageRaster.getRecommendedBarcodeWidth(),
                    barcodeImageRaster.getRecommendedBarcodeHeight(),
                    hints);
            barcodeQR = multiFormatWriter.encode(
                    barCodeContents, BarcodeFormat.QR_CODE,
                    barcodeImageRaster.getRecommendedBarcodeWidth(),
                    barcodeImageRaster.getRecommendedBarcodeHeight(),
                    hints);

            return barcodeImageRaster.renderToImage(barcode2D, barcodeQR, label);
        } catch (WriterException e) {
            throw new BarcodeGenerationException(e);
        }
    }


    private static class BarcodeImageRaster {
        static final int PADDING = 10;
        private final int textHeight;
        private final int subImageWidth;
        private final int subImageHeight;

        private final BufferedImage finalImage;
        private final BufferedImage topLeftImage;
        private final BufferedImage topRightImage;
        private final BufferedImage bottomLeftImage;
        private final BufferedImage bottomRightImage;
        private final Map<BufferedImage, Graphics2D> graphicsContext;


        BarcodeImageRaster(int width, int height) {
            textHeight = height/8;
            subImageWidth = width/2 - PADDING;
            subImageHeight = height/2 - PADDING;

            finalImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g2d = (Graphics2D) finalImage.getGraphics();
            g2d.setPaint(Color.white);
            g2d.fillRect(0, 0, finalImage.getWidth(), finalImage.getHeight());

            topLeftImage = finalImage.getSubimage(PADDING, PADDING, subImageWidth, subImageHeight);
            topRightImage = finalImage.getSubimage(width / 2, PADDING, subImageWidth, subImageHeight);
            bottomLeftImage = finalImage.getSubimage( PADDING , height/2, subImageWidth, subImageHeight);
            bottomRightImage = finalImage.getSubimage( width / 2, height/2, subImageWidth, subImageHeight);


            graphicsContext = new HashMap<>();
            graphicsContext.put(topLeftImage, createGraphicContext(topLeftImage));
            graphicsContext.put(topRightImage, createGraphicContext(topRightImage));
            graphicsContext.put(bottomLeftImage, createGraphicContext(bottomLeftImage));
            graphicsContext.put(bottomRightImage, createGraphicContext(bottomRightImage));
        }

        private Graphics2D createGraphicContext(BufferedImage image) {
            Graphics2D g2d = image.createGraphics();
            g2d.setFont(getStandardFontFromResources());
            RenderingHints rh = new RenderingHints(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHints(rh);
            return g2d;
        }

        @SuppressWarnings("Duplicates")
        private Font getStandardFontFromResources() {
            InputStream is = this.getClass().getResourceAsStream("/barcode_title.ttf");
            Font font = null;
            if (is != null) {
                try {
                    font = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont((long)this.textHeight - 10);
                } catch (FontFormatException | IOException e) {
                    throw new IllegalArgumentException("Could not find font in Classpath");
                }
            }

            return font;
        }

        private int getRecommendedBarcodeWidth() {
            return subImageWidth;
        }

        private int getRecommendedBarcodeHeight() {
            return subImageHeight - textHeight;
        }

        @SuppressWarnings("Duplicates")
        BufferedImage renderToImage(BitMatrix barcode1, BitMatrix barcode2, String label) {
            // Write the barcode in the top left and bottom right corner
            renderBarcodeToSubImage(topLeftImage, barcode1, label);
            renderBarcodeToSubImage(topRightImage, barcode2, label);
            renderBarcodeToSubImage(bottomLeftImage, barcode2, label);
            renderBarcodeToSubImage(bottomRightImage, barcode1, label);

            return finalImage;
        }

        private void renderBarcodeToSubImage(BufferedImage subImage, BitMatrix barcode, String label) {
            int width = barcode.getWidth();
            int height = barcode.getHeight();
            int[] pixels = new int[width * height];
            int index = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    pixels[index++] = barcode.get(x, y) ? Color.black.getRGB() : Color.white.getRGB();
                }
            }
            subImage.setRGB(0, textHeight, width, height, pixels, 0, width);

            Graphics2D g2d = graphicsContext.get(subImage);

            //Draw the human readable test
            g2d.setPaint(Color.white);
            g2d.fillRect(0, 0, topLeftImage.getWidth(), textHeight);
            g2d.setPaint(Color.black);
            int x = (topLeftImage.getWidth() - g2d.getFontMetrics().stringWidth(label)) / 2;
            int y = textHeight - 5;
            g2d.drawString(label, x, y);
        }
    }


    private class BarcodeGenerationException extends Exception {
        BarcodeGenerationException(WriterException e) {
            super(e);
        }
    }


    public static void main(String[] args) throws VideoRecorderException {
        TimeSource fakeTimeSource = new FakeTimeSource();

        ImageInput barcodeInput = new GenerateInputWithMatrixOfBarcodes(
                600, 400, fakeTimeSource);

        VideoRecorder videoRecorder = new VideoRecorder.Builder(barcodeInput)
                .withTimeSource(fakeTimeSource).build();
        // Capture video
        videoRecorder.open(BARCODE_VIDEO_PATH, 5, 4);
        videoRecorder.start(Duration.of(2, ChronoUnit.SECONDS));
        videoRecorder.close();
    }
}
