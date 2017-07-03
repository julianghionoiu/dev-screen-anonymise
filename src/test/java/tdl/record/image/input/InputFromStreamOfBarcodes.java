package tdl.record.image.input;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import tdl.record.time.TimeSource;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class InputFromStreamOfBarcodes implements ImageInput {
    private final BarcodeFormat barcodeFormat;
    private final int width;
    private final int height;
    private final BarcodeImageRaster barcodeImageRaster;
    private final TimeSource timeSource;
    private long systemStartTime;

    public InputFromStreamOfBarcodes(BarcodeFormat barcodeFormat, int width, int height, TimeSource timeSource) {
        this.width = width;
        this.height = height;
        this.barcodeImageRaster = new BarcodeImageRaster(width, height, this.height / 3);
        this.timeSource = timeSource;
        this.barcodeFormat = barcodeFormat;
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
            return renderAsBarcode(relativeSystemTime, TimeUnit.NANOSECONDS);
        } catch (BarcodeGenerationException e) {
            throw new InputImageGenerationException(e);
        }
    }

    @Override
    public BufferedImage getSampleImage() throws InputImageGenerationException {
        try {
            return renderAsBarcode(0, TimeUnit.NANOSECONDS);
        } catch (BarcodeGenerationException e) {
            throw new InputImageGenerationException(e);
        }
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

    private BufferedImage renderAsBarcode(long timestamp, TimeUnit timeUnit) throws BarcodeGenerationException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        BitMatrix matrix;
        try {
            String barCodeContents = Long.toString(timestamp);
            matrix = new MultiFormatWriter().encode(
                    barCodeContents, barcodeFormat, width, height, hints);
            long seconds = timeUnit.toSeconds(timestamp);
            long millis = timeUnit.toMillis(timestamp) - TimeUnit.SECONDS.toMillis(seconds);
            String timeAsMillis = String.format("%d.%03d", seconds, millis);
            return barcodeImageRaster.renderToImage(matrix, timeAsMillis);
        } catch (WriterException e) {
            throw new BarcodeGenerationException(e);
        }
    }


    private static class BarcodeImageRaster {
        final BufferedImage image;
        final Graphics2D g2d;
        final FontMetrics fm;
        private final int textHeight;

        BarcodeImageRaster(int width, int totalHeight, int textHeight) {
            image = new BufferedImage(width, totalHeight, BufferedImage.TYPE_3BYTE_BGR);
            g2d = image.createGraphics();
            this.textHeight = textHeight;
            g2d.setFont(new Font("Serif", Font.BOLD, this.textHeight - 10));
            fm = g2d.getFontMetrics();

            RenderingHints rh = new RenderingHints(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHints(rh);
        }

        BufferedImage renderToImage(BitMatrix matrix, String text) {
            if (matrix.getWidth() != image.getWidth()) {
                throw new IllegalArgumentException("Widths to not match, " + matrix.getWidth() + " vs " + image.getWidth());
            }
            if (matrix.getHeight() != image.getHeight()) {
                throw new IllegalArgumentException("Heights to not match, " + matrix.getHeight() + " vs " + image.getHeight());
            }

            // Write the barcode
            int width = matrix.getWidth();
            int height = matrix.getHeight();
            int[] pixels = new int[width * height];
            int index = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    pixels[index++] = matrix.get(x, y) ? Color.black.getRGB() : Color.white.getRGB();
                }
            }
            image.setRGB(0, 0, width, height, pixels, 0, width);

            //Draw the human readable test
            g2d.setPaint(Color.white);
            g2d.fillRect(0, 0, image.getWidth(), textHeight);
            g2d.setPaint(Color.black);
            int x = (image.getWidth() - fm.stringWidth(text)) / 2;
            int y = textHeight - 5;
            g2d.drawString(text, x, y);

            return image;
        }
    }


    private class BarcodeGenerationException extends Exception {

        BarcodeGenerationException(WriterException e) {
            super(e);
        }
    }
}
