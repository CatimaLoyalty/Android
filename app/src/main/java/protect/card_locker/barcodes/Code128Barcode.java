package protect.card_locker.barcodes;

import com.google.zxing.BarcodeFormat;

public class Code128Barcode extends Barcode {
    @Override
    public String prettyName() {
        return "Code 128";
    }

    @Override
    public BarcodeFormat format() {
        return BarcodeFormat.CODE_128;
    }

    @Override
    public String exampleValue() {
        return "CODE_128";
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
