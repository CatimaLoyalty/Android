package protect.card_locker.barcodes;

import com.google.zxing.BarcodeFormat;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BarcodeFactory {
    public static final Map<String, BarcodeFormat> barcodeNames = new HashMap<>() {{
        put(BarcodeFormat.AZTEC.name(), BarcodeFormat.AZTEC);
        put(BarcodeFormat.CODE_39.name(), BarcodeFormat.CODE_39);
        put(BarcodeFormat.CODE_93.name(), BarcodeFormat.CODE_93);
        put(BarcodeFormat.CODE_128.name(), BarcodeFormat.CODE_128);
        put(BarcodeFormat.CODABAR.name(), BarcodeFormat.CODABAR);
        put(BarcodeFormat.DATA_MATRIX.name(), BarcodeFormat.DATA_MATRIX);
        put(BarcodeFormat.EAN_8.name(), BarcodeFormat.EAN_8);
        put(BarcodeFormat.EAN_13.name(), BarcodeFormat.EAN_13);
        put(BarcodeFormat.ITF.name(), BarcodeFormat.ITF);
        put(BarcodeFormat.PDF_417.name(), BarcodeFormat.PDF_417);
        put(BarcodeFormat.QR_CODE.name(), BarcodeFormat.QR_CODE);
        put(BarcodeFormat.UPC_A.name(), BarcodeFormat.UPC_A);
        put(BarcodeFormat.UPC_E.name(), BarcodeFormat.UPC_E);
    }};

    public static Barcode fromBarcode(BarcodeFormat barcodeFormat) {
        switch (barcodeFormat) {
            case AZTEC: return new AztecBarcode();
            case CODE_39: return new Code39Barcode();
            case CODE_93: return new Code93Barcode();
            case CODE_128: return new Code128Barcode();
            case CODABAR: return new CodabarBarcode();
            case DATA_MATRIX: return new DataMatrixBarcode();
            case EAN_8: return new Ean8Barcode();
            case EAN_13: return new Ean13Barcode();
            case ITF: return new ItfBarcode();
            case PDF_417: return new Pdf417Barcode();
            case QR_CODE: return new QrCodeBarcode();
            case UPC_A: return new UpcABarcode();
            case UPC_E: return new UpcEBarcode();
            default: throw new IllegalArgumentException();
        }
    }

    public static Barcode fromName(String name) {
        return fromBarcode(Objects.requireNonNull(barcodeNames.get(name)));
    }

    public static boolean isSupported(BarcodeFormat barcodeFormat) {
        return barcodeNames.containsValue(barcodeFormat);
    }

    public static boolean isSupported(String name) {
        return barcodeNames.containsKey(name);
    }

    public static Collection<BarcodeFormat> getAllFormats() {
        return barcodeNames.values();
    }
}
