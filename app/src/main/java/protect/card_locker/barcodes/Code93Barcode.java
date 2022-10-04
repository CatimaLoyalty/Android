package protect.card_locker.barcodes;

import com.google.zxing.BarcodeFormat;

public class Code93Barcode extends Barcode {
    @Override
    public String prettyName() {
        return "Code 93";
    }

    @Override
    public BarcodeFormat format() {
        return BarcodeFormat.CODE_93;
    }

    @Override
    public String exampleValue() {
        return "CODE_93";
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
