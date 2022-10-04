package protect.card_locker.barcodes;

import com.google.zxing.BarcodeFormat;

public class UpcABarcode extends Barcode {
    @Override
    public String prettyName() {
        return "UPC A";
    }

    @Override
    public BarcodeFormat format() {
        return BarcodeFormat.UPC_A;
    }

    @Override
    public String exampleValue() {
        return "123456789012";
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
