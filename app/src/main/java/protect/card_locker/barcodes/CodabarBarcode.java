package protect.card_locker.barcodes;

import com.google.zxing.BarcodeFormat;

public class CodabarBarcode extends Barcode {
    @Override
    public String prettyName() {
        return "Codabar";
    }

    @Override
    public BarcodeFormat format() {
        return BarcodeFormat.CODABAR;
    }

    @Override
    public String exampleValue() {
        return "C0C";
    }

    @Override
    public boolean isSquare() {
        return false;
    }

    @Override
    public boolean is2D() {
        return false;
    }

    @Override
    public boolean hasInternalPadding() {
        return false;
    }
}
