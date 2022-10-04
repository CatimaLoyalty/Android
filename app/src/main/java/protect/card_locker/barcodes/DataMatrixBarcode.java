package protect.card_locker.barcodes;

import com.google.zxing.BarcodeFormat;

public class DataMatrixBarcode extends Barcode {
    @Override
    public String prettyName() {
        return "Data Matrix";
    }

    @Override
    public BarcodeFormat format() {
        return BarcodeFormat.DATA_MATRIX;
    }

    @Override
    public String exampleValue() {
        return "DATA_MATRIX";
    }

    @Override
    public boolean isSquare() {
        return true;
    }

    @Override
    public boolean is2D() {
        return true;
    }
}
