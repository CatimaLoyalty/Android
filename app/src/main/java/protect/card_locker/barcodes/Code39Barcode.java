package protect.card_locker.barcodes;

import com.google.zxing.BarcodeFormat;

public class Code39Barcode extends Barcode {
    @Override
    public String prettyName() {
        return "Code 39";
    }

    @Override
    public BarcodeFormat format() {
        return BarcodeFormat.CODE_39;
    }

    @Override
    public String exampleValue() {
        return "CODE_39";
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
