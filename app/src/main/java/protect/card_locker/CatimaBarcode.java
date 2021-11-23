package protect.card_locker;

import com.google.zxing.BarcodeFormat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CatimaBarcode {
    public static final List<BarcodeFormat> barcodeFormats = Collections.unmodifiableList(Arrays.asList(
            BarcodeFormat.AZTEC,
            BarcodeFormat.CODE_39,
            BarcodeFormat.CODE_128,
            BarcodeFormat.CODABAR,
            BarcodeFormat.DATA_MATRIX,
            BarcodeFormat.EAN_8,
            BarcodeFormat.EAN_13,
            BarcodeFormat.ITF,
            BarcodeFormat.PDF_417,
            BarcodeFormat.QR_CODE,
            BarcodeFormat.UPC_A,
            BarcodeFormat.UPC_E
    ));

    public static final List<String> barcodePrettyNames = Collections.unmodifiableList(Arrays.asList(
            "Aztec",
            "Code 39",
            "Code 128",
            "Codabar",
            "Data Matrix",
            "EAN 8",
            "EAN 13",
            "ITF",
            "PDF 417",
            "QR Code",
            "UPC A",
            "UPC E"
    ));

    private final BarcodeFormat mBarcodeFormat;

    private CatimaBarcode(BarcodeFormat barcodeFormat) {
        mBarcodeFormat = barcodeFormat;
    }

    public static CatimaBarcode fromBarcode(BarcodeFormat barcodeFormat) {
        return new CatimaBarcode(barcodeFormat);
    }

    public static CatimaBarcode fromName(String name) {
        return new CatimaBarcode(BarcodeFormat.valueOf(name));
    }

    public static CatimaBarcode fromPrettyName(String prettyName) {
        try {
            return new CatimaBarcode(barcodeFormats.get(barcodePrettyNames.indexOf(prettyName)));
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("No barcode type with pretty name " + prettyName + " known!");
        }
    }

    public boolean isSupported() {
        return barcodeFormats.contains(mBarcodeFormat);
    }

    public boolean isSquare() {
        return mBarcodeFormat == BarcodeFormat.AZTEC
                || mBarcodeFormat == BarcodeFormat.DATA_MATRIX
                || mBarcodeFormat == BarcodeFormat.MAXICODE
                || mBarcodeFormat == BarcodeFormat.QR_CODE;
    }

    public BarcodeFormat format() {
        return mBarcodeFormat;
    }

    public String name() {
        return mBarcodeFormat.name();
    }

    public String prettyName() {
        int index = barcodeFormats.indexOf(mBarcodeFormat);

        if (index == -1 || index >= barcodePrettyNames.size()) {
            return mBarcodeFormat.name();
        }

        return barcodePrettyNames.get(index);
    }
}
