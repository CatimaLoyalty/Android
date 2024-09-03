package protect.card_locker;

import androidx.annotation.Nullable;

public class BarcodeValues {
    @Nullable
    private final CatimaBarcode mFormat;
    private final String mContent;
    private String mNote;

    public BarcodeValues(@Nullable CatimaBarcode format, String content) {
        mFormat = format;
        mContent = content;
    }

    public void setNote(String note) {
        mNote = note;
    }

    public @Nullable CatimaBarcode format() {
        return mFormat;
    }

    public String content() {
        return mContent;
    }

    public String note() { return mNote; }
}