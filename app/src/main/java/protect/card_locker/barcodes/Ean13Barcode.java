package protect.card_locker.barcodes;

import com.google.zxing.BarcodeFormat;

public class Ean13Barcode extends Barcode {
    @Override
    public String prettyName() {
        return "EAN 13";
    }

    @Override
    public BarcodeFormat format() {
        return BarcodeFormat.EAN_13;
    }

    @Override
    public String exampleValue() {
        return "5901234123457";
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
