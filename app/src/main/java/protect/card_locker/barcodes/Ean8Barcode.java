package protect.card_locker.barcodes;

import com.google.zxing.BarcodeFormat;

public class Ean8Barcode extends Barcode {
    @Override
    public String prettyName() {
        return "EAN 8";
    }

    @Override
    public BarcodeFormat format() {
        return BarcodeFormat.EAN_8;
    }

    @Override
    public String exampleValue() {
        return "32123456";
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
