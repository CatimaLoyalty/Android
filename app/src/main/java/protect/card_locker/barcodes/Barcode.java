package protect.card_locker.barcodes;

import com.google.zxing.BarcodeFormat;

/**
 * Abstract barcode class
 */
public abstract class Barcode {
    public String name() {
        return format().name();
    };
    abstract public String prettyName();
    abstract public BarcodeFormat format();
    abstract public String exampleValue();

    abstract public boolean isSquare();
    abstract public boolean is2D();
    public boolean hasInternalPadding() {
        return false;
    };
    public boolean isSupported() { return true; };
}
