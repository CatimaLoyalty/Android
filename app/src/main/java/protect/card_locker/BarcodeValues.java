package protect.card_locker;

public class BarcodeValues {
    private final String mFormat;
    private final String mContent;

    public BarcodeValues(String format, String content) {
        mFormat = format;
        mContent = content;
    }

    public String format() {
        return mFormat;
    }

    public String content() {
        return mContent;
    }

    public boolean isEmpty() {
        return mFormat == null && mContent == null;
    }
}
