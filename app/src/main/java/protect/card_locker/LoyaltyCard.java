package protect.card_locker;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.google.zxing.BarcodeFormat;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Date;

public class LoyaltyCard implements Parcelable {
    public final int id;
    public final String store;
    public final String note;
    public final Date expiry;
    public final BigDecimal balance;
    public final Currency balanceType;
    public final String cardId;

    @Nullable
    public final String barcodeId;

    @Nullable
    public final BarcodeFormat barcodeType;

    @Nullable
    public final Integer headerColor;

    public final int starStatus;

    public LoyaltyCard(final int id, final String store, final String note, final Date expiry,
                       final BigDecimal balance, final Currency balanceType, final String cardId,
                       @Nullable final String barcodeId, @Nullable final BarcodeFormat barcodeType, @Nullable final Integer headerColor,
                       final int starStatus)
    {
        this.id = id;
        this.store = store;
        this.note = note;
        this.expiry = expiry;
        this.balance = balance;
        this.balanceType = balanceType;
        this.cardId = cardId;
        this.barcodeId = barcodeId;
        this.barcodeType = barcodeType;
        this.headerColor = headerColor;
        this.starStatus = starStatus;
    }

    protected LoyaltyCard(Parcel in) {
        id = in.readInt();
        store = in.readString();
        note = in.readString();
        long tmpExpiry = in.readLong();
        expiry = tmpExpiry != -1 ? new Date(tmpExpiry) : null;
        balance = (BigDecimal) in.readValue(BigDecimal.class.getClassLoader());
        balanceType = (Currency) in.readValue(Currency.class.getClassLoader());
        cardId = in.readString();
        barcodeId = in.readString();
        String tmpBarcodeType = in.readString();
        barcodeType = !tmpBarcodeType.isEmpty() ? BarcodeFormat.valueOf(tmpBarcodeType) : null;
        int tmpHeaderColor = in.readInt();
        headerColor = tmpHeaderColor != -1 ? tmpHeaderColor : null;
        starStatus = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(store);
        parcel.writeString(note);
        parcel.writeLong(expiry != null ? expiry.getTime() : -1);
        parcel.writeValue(balance);
        parcel.writeValue(balanceType);
        parcel.writeString(cardId);
        parcel.writeString(barcodeId);
        parcel.writeString(barcodeType != null ? barcodeType.toString() : "");
        parcel.writeInt(headerColor != null ? headerColor : -1);
        parcel.writeInt(starStatus);
    }

    public static LoyaltyCard toLoyaltyCard(Cursor cursor)
    {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.ID));
        String store = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.STORE));
        String note = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.NOTE));
        long expiryLong = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.EXPIRY));
        BigDecimal balance = new BigDecimal(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BALANCE)));
        String cardId = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.CARD_ID));
        String barcodeId = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BARCODE_ID));
        int starred = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.STAR_STATUS));

        int barcodeTypeColumn = cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BARCODE_TYPE);
        int balanceTypeColumn = cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BALANCE_TYPE);
        int headerColorColumn = cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.HEADER_COLOR);

        BarcodeFormat barcodeType = null;
        Currency balanceType = null;
        Date expiry = null;
        Integer headerColor = null;

        if (cursor.isNull(barcodeTypeColumn) == false)
        {
            barcodeType = BarcodeFormat.valueOf(cursor.getString(barcodeTypeColumn));
        }

        if (cursor.isNull(balanceTypeColumn) == false)
        {
            balanceType = Currency.getInstance(cursor.getString(balanceTypeColumn));
        }

        if(expiryLong > 0)
        {
            expiry = new Date(expiryLong);
        }

        if(cursor.isNull(headerColorColumn) == false)
        {
            headerColor = cursor.getInt(headerColorColumn);
        }

        return new LoyaltyCard(id, store, note, expiry, balance, balanceType, cardId, barcodeId, barcodeType, headerColor, starred);
    }

    @Override
    public int describeContents() {
        return id;
    }

    public static final Creator<LoyaltyCard> CREATOR = new Creator<LoyaltyCard>() {
        @Override
        public LoyaltyCard createFromParcel(Parcel in) {
            return new LoyaltyCard(in);
        }

        @Override
        public LoyaltyCard[] newArray(int size) {
            return new LoyaltyCard[size];
        }
    };
}
