package tdl.record.image.output;

import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import tdl.record.time.TimeSource;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class OutputToBarcodeReader implements ImageOutput {
    private final TimeSource timeSource;
    private final List<TimestampPair> decodedBarcodes;
    private final BarcodeFormat barcodeFormat;

    public OutputToBarcodeReader(TimeSource timeSource, BarcodeFormat barcodeFormat) {
        this.timeSource = timeSource;
        this.barcodeFormat = barcodeFormat;
        this.decodedBarcodes = new ArrayList<>();
    }

    @Override
    public void open() {
    }

    @Override
    public BufferedImage getSuggestedOutputSample(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
    }

    @Override
    public void writeImage(BufferedImage image) {
        long systemTimeNano = timeSource.currentTimeNano();

        try {
            long barcodeTimeNano = Long.parseLong(decodeBarcode(image, barcodeFormat));
            decodedBarcodes.add(new TimestampPair(systemTimeNano, barcodeTimeNano));
        } catch (IOException | NotFoundException | FormatException e) {
            System.err.println("Could not extract barcode at: "+systemTimeNano);
        }
    }

    @Override
    public void close() {
        //Nothing to close
    }

    public List<TimestampPair> getDecodedBarcodes() {
        return decodedBarcodes;
    }

    public static class TimestampPair {
        public final Long systemTimestamp;
        public final Long barcodeTimestamp;

        TimestampPair(Long systemTimestamp, Long barcodeTimestamp) {
            this.systemTimestamp = systemTimestamp;
            this.barcodeTimestamp = barcodeTimestamp;
        }
    }

    private static String decodeBarcode(BufferedImage image, final BarcodeFormat format)
            throws IOException, NotFoundException, FormatException {
        ArrayList<BarcodeFormat> barcodeFormats = new ArrayList<BarcodeFormat>() {{
            add(format);
        }};
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, barcodeFormats);

        LuminanceSource source;
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(),
                null, 0, image.getWidth());
        source = new RGBLuminanceSource(image.getWidth(), image.getHeight(), pixels);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        MultiFormatReader multiFormatReader = new MultiFormatReader();
        Result result = multiFormatReader.decode(bitmap, hints);

        return result.getText();
    }
}
