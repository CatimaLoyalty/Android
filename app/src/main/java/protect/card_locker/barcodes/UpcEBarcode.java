package protect.card_locker.barcodes;

import com.google.zxing.BarcodeFormat;

public class UpcEBarcode extends Barcode {
    @Override
    public String prettyName() {
        return "UPC E";
    }

    @Override
    public BarcodeFormat format() {
        return BarcodeFormat.UPC_E;
    }

    @Override
    public String exampleValue() {
        return "0123456";
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
