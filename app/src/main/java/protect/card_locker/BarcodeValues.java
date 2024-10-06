package protect.card_locker;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;

public class BarcodeValues implements Parcelable {
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

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable. Creator<BarcodeValues> CREATOR
            = new Parcelable. Creator<BarcodeValues>() {
        public BarcodeValues createFromParcel(Parcel in) {
            return new BarcodeValues(in);
        }

        public BarcodeValues[] newArray(int size) {
            return new BarcodeValues[size];
        }
    };
    public BarcodeValues(Parcel from){
        this(CatimaBarcode.fromName(from.readString()), from.readString());
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mFormat.name());
        dest.writeString(mContent);
    }
}