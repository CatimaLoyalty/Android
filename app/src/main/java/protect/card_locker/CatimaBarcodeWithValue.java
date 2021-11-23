package protect.card_locker;

public class CatimaBarcodeWithValue {
    private final CatimaBarcode mCatimaBarcode;
    private final String mValue;

    public CatimaBarcodeWithValue(CatimaBarcode catimaBarcode, String value) {
        mCatimaBarcode = catimaBarcode;
        mValue = value;
    }

    public CatimaBarcode catimaBarcode() {
        return mCatimaBarcode;
    }

    public String value() {
        return mValue;
    }
}
