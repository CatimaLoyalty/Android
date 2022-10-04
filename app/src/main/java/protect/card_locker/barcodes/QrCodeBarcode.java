package protect.card_locker.barcodes;

import com.google.zxing.BarcodeFormat;

public class QrCodeBarcode extends Barcode {
    @Override
    public String prettyName() {
        return "QR Code";
    }

    @Override
    public BarcodeFormat format() {
        return BarcodeFormat.QR_CODE;
    }

    @Override
    public String exampleValue() {
        return "QR_CODE";
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
        return true;
    }
}
