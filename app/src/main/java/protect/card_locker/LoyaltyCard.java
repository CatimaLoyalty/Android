package protect.card_locker;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LoyaltyCard implements Parcelable {
    public final int id;
    public final String store;
    public final String note;
    @Nullable
    public final Date validFrom;
    @Nullable
    public final Date expiry;
    public final BigDecimal balance;
    @Nullable
    public final Currency balanceType;
    public final String cardId;
    @Nullable
    public final String barcodeId;
    @Nullable
    public final CatimaBarcode barcodeType;
    @Nullable
    public final Integer headerColor;
    public final int starStatus;
    public final int archiveStatus;
    public final long lastUsed;
    public int zoomLevel;

    public LoyaltyCard(final int id, final String store, final String note, @Nullable final Date validFrom,
                       @Nullable final Date expiry, final BigDecimal balance, @Nullable final Currency balanceType,
                       final String cardId, @Nullable final String barcodeId, @Nullable final CatimaBarcode barcodeType,
                       @Nullable final Integer headerColor, final int starStatus,
                       final long lastUsed, final int zoomLevel, final int archiveStatus) {
        this.id = id;
        this.store = store;
        this.note = note;
        this.validFrom = validFrom;
        this.expiry = expiry;
        this.balance = balance;
        this.balanceType = balanceType;
        this.cardId = cardId;
        this.barcodeId = barcodeId;
        this.barcodeType = barcodeType;
        this.headerColor = headerColor;
        this.starStatus = starStatus;
        this.lastUsed = lastUsed;
        this.zoomLevel = zoomLevel;
        this.archiveStatus = archiveStatus;
    }

    protected LoyaltyCard(Parcel in) {
        id = in.readInt();
        store = in.readString();
        note = in.readString();
        long tmpValidFrom = in.readLong();
        validFrom = tmpValidFrom != -1 ? new Date(tmpValidFrom) : null;
        long tmpExpiry = in.readLong();
        expiry = tmpExpiry != -1 ? new Date(tmpExpiry) : null;
        balance = (BigDecimal) in.readValue(BigDecimal.class.getClassLoader());
        balanceType = (Currency) in.readValue(Currency.class.getClassLoader());
        cardId = in.readString();
        barcodeId = in.readString();
        String tmpBarcodeType = in.readString();
        barcodeType = !tmpBarcodeType.isEmpty() ? CatimaBarcode.fromName(tmpBarcodeType) : null;
        int tmpHeaderColor = in.readInt();
        headerColor = tmpHeaderColor != -1 ? tmpHeaderColor : null;
        starStatus = in.readInt();
        lastUsed = in.readLong();
        zoomLevel = in.readInt();
        archiveStatus = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(store);
        parcel.writeString(note);
        parcel.writeLong(validFrom != null ? validFrom.getTime() : -1);
        parcel.writeLong(expiry != null ? expiry.getTime() : -1);
        parcel.writeValue(balance);
        parcel.writeValue(balanceType);
        parcel.writeString(cardId);
        parcel.writeString(barcodeId);
        parcel.writeString(barcodeType != null ? barcodeType.name() : "");
        parcel.writeInt(headerColor != null ? headerColor : -1);
        parcel.writeInt(starStatus);
        parcel.writeLong(lastUsed);
        parcel.writeInt(zoomLevel);
        parcel.writeInt(archiveStatus);
    }

    public static LoyaltyCard toLoyaltyCard(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.ID));
        String store = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.STORE));
        String note = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.NOTE));
        long validFromLong = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.VALID_FROM));
        long expiryLong = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.EXPIRY));
        BigDecimal balance = new BigDecimal(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BALANCE)));
        String cardId = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.CARD_ID));
        String barcodeId = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BARCODE_ID));
        int starred = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.STAR_STATUS));
        long lastUsed = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.LAST_USED));
        int zoomLevel = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.ZOOM_LEVEL));
        int archived = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.ARCHIVE_STATUS));

        int barcodeTypeColumn = cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BARCODE_TYPE);
        int balanceTypeColumn = cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BALANCE_TYPE);
        int headerColorColumn = cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.HEADER_COLOR);

        CatimaBarcode barcodeType = null;
        Currency balanceType = null;
        Date validFrom = null;
        Date expiry = null;
        Integer headerColor = null;

        if (cursor.isNull(barcodeTypeColumn) == false) {
            barcodeType = CatimaBarcode.fromName(cursor.getString(barcodeTypeColumn));
        }

        if (cursor.isNull(balanceTypeColumn) == false) {
            balanceType = Currency.getInstance(cursor.getString(balanceTypeColumn));
        }

        if (validFromLong > 0) {
            validFrom = new Date(validFromLong);
        }

        if (expiryLong > 0) {
            expiry = new Date(expiryLong);
        }

        if (cursor.isNull(headerColorColumn) == false) {
            headerColor = cursor.getInt(headerColorColumn);
        }

        return new LoyaltyCard(id, store, note, validFrom, expiry, balance, balanceType, cardId, barcodeId, barcodeType, headerColor, starred, lastUsed, zoomLevel, archived);
    }

    public static boolean isDuplicate(final LoyaltyCard a, final LoyaltyCard b) {
        // Skip lastUsed & zoomLevel
        return a.id == b.id && // non-nullable int
                a.store.equals(b.store) && // non-nullable String
                a.note.equals(b.note) && // non-nullable String
                Utils.equals(a.validFrom, b.validFrom) && // nullable Date
                Utils.equals(a.expiry, b.expiry) && // nullable Date
                a.balance.equals(b.balance) && // non-nullable BigDecimal
                Utils.equals(a.balanceType, b.balanceType) && // nullable Currency
                a.cardId.equals(b.cardId) && // non-nullable String
                Utils.equals(a.barcodeId, b.barcodeId) && // nullable String
                Utils.equals(a.barcodeType == null ? null : a.barcodeType.format(),
                        b.barcodeType == null ? null : b.barcodeType.format()) && // nullable CatimaBarcode with no overridden .equals(), so we need to check .format()
                Utils.equals(a.headerColor, b.headerColor) && // nullable Integer
                a.starStatus == b.starStatus && // non-nullable int
                a.archiveStatus == b.archiveStatus; // non-nullable int
    }

    @Override
    public int describeContents() {
        return id;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(
                "LoyaltyCard{%n  id=%s,%n  store=%s,%n  note=%s,%n  validFrom=%s,%n  expiry=%s,%n"
                        + "  balance=%s,%n  balanceType=%s,%n  cardId=%s,%n  barcodeId=%s,%n  barcodeType=%s,%n"
                        + "  headerColor=%s,%n  starStatus=%s,%n  lastUsed=%s,%n  zoomLevel=%s,%n  archiveStatus=%s%n}",
                this.id,
                this.store,
                this.note,
                this.validFrom,
                this.expiry,
                this.balance,
                this.balanceType,
                this.cardId,
                this.barcodeId,
                this.barcodeType != null ? this.barcodeType.format() : null,
                this.headerColor,
                this.starStatus,
                this.lastUsed,
                this.zoomLevel,
                this.archiveStatus
        );
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
