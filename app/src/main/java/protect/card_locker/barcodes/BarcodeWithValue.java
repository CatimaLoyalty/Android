package protect.card_locker.barcodes;

public class BarcodeWithValue {
    private final Barcode mBarcode;
    private final String mValue;

    public BarcodeWithValue(Barcode barcode, String value) {
        mBarcode = barcode;
        mValue = value;
    }

    public Barcode barcode() {
        return mBarcode;
    }

    public String value() {
        return mValue;
    }
}
