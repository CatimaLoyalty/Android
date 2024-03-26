package protect.card_locker;

public class BarcodeValues {
    private final String mFormat;
    private final String mContent;
    private String mNote;

    public BarcodeValues(String format, String content) {
        mFormat = format;
        mContent = content;
    }

    public void setNote(String note) {
        mNote = note;
    }

    public String format() {
        return mFormat;
    }

    public String content() {
        return mContent;
    }

    public String note() { return mNote; }
}