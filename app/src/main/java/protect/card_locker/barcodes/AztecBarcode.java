package protect.card_locker.barcodes;

import com.google.zxing.BarcodeFormat;

public class AztecBarcode extends Barcode {
    @Override
    public String prettyName() {
        return "Aztec";
    }

    @Override
    public BarcodeFormat format() {
        return BarcodeFormat.AZTEC;
    }

    @Override
    public String exampleValue() {
        return "AZTEC";
    }

    @Override
    public boolean isSquare() {
        return true;
    }

    @Override
    public boolean is2D() {
        return true;
    }

    @Override
    public boolean hasInternalPadding() {
        return false;
    }
}
