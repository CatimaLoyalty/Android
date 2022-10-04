package protect.card_locker.barcodes;

import com.google.zxing.BarcodeFormat;

public class ItfBarcode extends Barcode {
    @Override
    public String prettyName() {
        return "ITF";
    }

    @Override
    public BarcodeFormat format() {
        return BarcodeFormat.ITF;
    }

    @Override
    public String exampleValue() {
        return "1003";
    }

    @Override
    public boolean isSquare() {
        return false;
    }

    @Override
    public boolean is2D() {
        return false;
    }
}
