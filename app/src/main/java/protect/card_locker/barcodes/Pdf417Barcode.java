package protect.card_locker.barcodes;

import com.google.zxing.BarcodeFormat;

public class Pdf417Barcode extends Barcode {
    @Override
    public String prettyName() {
        return "PDF 417";
    }

    @Override
    public BarcodeFormat format() {
        return BarcodeFormat.PDF_417;
    }

    @Override
    public String exampleValue() {
        return "PDF_417";
    }

    @Override
    public boolean isSquare() {
        return false;
    }

    @Override
    public boolean is2D() {
        return true;
    }

    @Override
    public boolean hasInternalPadding() {
        return true;
    }
}
