package protect.card_locker;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LoyaltyCard implements Parcelable {
    public int id;
    public String store;
    public String note;
    @Nullable
    public Date validFrom;
    @Nullable
    public Date expiry;
    public BigDecimal balance;
    @Nullable
    public Currency balanceType;
    public String cardId;
    @Nullable
    public String barcodeId;
    @Nullable
    public CatimaBarcode barcodeType;
    @Nullable
    public Integer headerColor;
    public int starStatus;
    public long lastUsed;
    public int zoomLevel;
    public int archiveStatus;

    public static final String BUNDLE_LOYALTY_CARD_ID = "loyaltyCardId";
    public static final String BUNDLE_LOYALTY_CARD_STORE = "loyaltyCardStore";
    public static final String BUNDLE_LOYALTY_CARD_NOTE = "loyaltyCardNote";
    public static final String BUNDLE_LOYALTY_CARD_VALID_FROM = "loyaltyCardValidFrom";
    public static final String BUNDLE_LOYALTY_CARD_EXPIRY = "loyaltyCardExpiry";
    public static final String BUNDLE_LOYALTY_CARD_BALANCE = "loyaltyCardBalance";
    public static final String BUNDLE_LOYALTY_CARD_BALANCE_TYPE = "loyaltyCardBalanceType";
    public static final String BUNDLE_LOYALTY_CARD_CARD_ID = "loyaltyCardCardId";
    public static final String BUNDLE_LOYALTY_CARD_BARCODE_ID = "loyaltyCardBarcodeId";
    public static final String BUNDLE_LOYALTY_CARD_BARCODE_TYPE = "loyaltyCardBarcodeType";
    public static final String BUNDLE_LOYALTY_CARD_HEADER_COLOR = "loyaltyCardHeaderColor";
    public static final String BUNDLE_LOYALTY_CARD_STAR_STATUS = "loyaltyCardStarStatus";
    public static final String BUNDLE_LOYALTY_CARD_LAST_USED = "loyaltyCardLastUsed";
    public static final String BUNDLE_LOYALTY_CARD_ZOOM_LEVEL = "loyaltyCardZoomLevel";
    public static final String BUNDLE_LOYALTY_CARD_ARCHIVE_STATUS = "loyaltyCardArchiveStatus";

    /**
     * Create a loyalty card object with default values
     */
    public LoyaltyCard() {
        setId(-1);
        setStore("");
        setNote("");
        setValidFrom(null);
        setExpiry(null);
        setBalance(new BigDecimal("0"));
        setBalanceType(null);
        setCardId("");
        setBarcodeId(null);
        setBarcodeType(null);
        setHeaderColor(null);
        setStarStatus(0);
        setLastUsed(0);
        setZoomLevel(100);
        setArchiveStatus(0);
    }

    /**
     * Create a new loyalty card
     *
     * @param id
     * @param store
     * @param note
     * @param validFrom
     * @param expiry
     * @param balance
     * @param balanceType
     * @param cardId
     * @param barcodeId
     * @param barcodeType
     * @param headerColor
     * @param starStatus
     * @param lastUsed
     * @param zoomLevel
     * @param archiveStatus
     */
    public LoyaltyCard(final int id, final String store, final String note, @Nullable final Date validFrom,
                       @Nullable final Date expiry, final BigDecimal balance, @Nullable final Currency balanceType,
                       final String cardId, @Nullable final String barcodeId, @Nullable final CatimaBarcode barcodeType,
                       @Nullable final Integer headerColor, final int starStatus,
                       final long lastUsed, final int zoomLevel, final int archiveStatus) {
        setId(id);
        setStore(store);
        setNote(note);
        setValidFrom(validFrom);
        setExpiry(expiry);
        setBalance(balance);
        setBalanceType(balanceType);
        setCardId(cardId);
        setBarcodeId(barcodeId);
        setBarcodeType(barcodeType);
        setHeaderColor(headerColor);
        setStarStatus(starStatus);
        setLastUsed(lastUsed);
        setZoomLevel(zoomLevel);
        setArchiveStatus(archiveStatus);
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setStore(@NonNull String store) {
        this.store = store;
    }

    public void setNote(@Nullable String note) {
        this.note = note;
    }

    public void setValidFrom(@Nullable Date validFrom) {
        this.validFrom = validFrom;
    }

    public void setExpiry(@Nullable Date expiry) {
        this.expiry = expiry;
    }

    public void setBalance(@Nullable BigDecimal balance) {
        this.balance = balance;
    }

    public void setBalanceType(@Nullable Currency balanceType) {
        this.balanceType = balanceType;
    }

    public void setCardId(@NonNull String cardId) {
        this.cardId = cardId;
    }

    public void setBarcodeId(@Nullable String barcodeId) {
        this.barcodeId = barcodeId;
    }

    public void setBarcodeType(@Nullable CatimaBarcode barcodeType) {
        this.barcodeType = barcodeType;
    }

    public void setHeaderColor(@Nullable Integer headerColor) {
        this.headerColor = headerColor;
    }

    public void setStarStatus(int starStatus) {
        if (starStatus != 0 && starStatus != 1) {
            throw new IllegalArgumentException("starStatus must be 0 or 1");
        }

        this.starStatus = starStatus;
    }

    public void setLastUsed(long lastUsed) {
        this.lastUsed = lastUsed;
    }

    public void setZoomLevel(int zoomLevel) {
        if (zoomLevel < 0 || zoomLevel > 100) {
            throw new IllegalArgumentException("zoomLevel must be in range 0-100");
        }

        this.zoomLevel = zoomLevel;
    }

    public void setArchiveStatus(int archiveStatus) {
        if (archiveStatus != 0 && archiveStatus != 1) {
            throw new IllegalArgumentException("archiveStatus must be 0 or 1");
        }

        this.archiveStatus = archiveStatus;
    }

    protected LoyaltyCard(Parcel in) {
        setId(in.readInt());
        setStore(Objects.requireNonNull(in.readString()));
        setNote(in.readString());
        long tmpValidFrom = in.readLong();
        setValidFrom(tmpValidFrom != -1 ? new Date(tmpValidFrom) : null);
        long tmpExpiry = in.readLong();
        setExpiry(tmpExpiry != -1 ? new Date(tmpExpiry) : null);
        setBalance((BigDecimal) in.readValue(BigDecimal.class.getClassLoader()));
        setBalanceType((Currency) in.readValue(Currency.class.getClassLoader()));
        setCardId(Objects.requireNonNull(in.readString()));
        setBarcodeId(in.readString());
        String tmpBarcodeType = in.readString();
        setBarcodeType((tmpBarcodeType != null && !tmpBarcodeType.isEmpty()) ? CatimaBarcode.fromName(tmpBarcodeType) : null);
        int tmpHeaderColor = in.readInt();
        setHeaderColor(tmpHeaderColor != -1 ? tmpHeaderColor : null);
        setStarStatus(in.readInt());
        setLastUsed(in.readLong());
        setZoomLevel(in.readInt());
        setArchiveStatus(in.readInt());
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

    public static LoyaltyCard fromBundle(Bundle bundle) {
        // Grab default card
        LoyaltyCard loyaltyCard = new LoyaltyCard();

        // Update from bundle
        loyaltyCard.updateFromBundle(bundle);

        // Return updated version
        return loyaltyCard;
    }

    public void updateFromBundle(Bundle bundle) {
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_ID)) {
            setId(bundle.getInt(BUNDLE_LOYALTY_CARD_ID));
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_STORE)) {
            setStore(Objects.requireNonNull(bundle.getString(BUNDLE_LOYALTY_CARD_STORE)));
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_NOTE)) {
            setNote(bundle.getString(BUNDLE_LOYALTY_CARD_NOTE));
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_VALID_FROM)) {
            long tmpValidFrom = bundle.getLong(BUNDLE_LOYALTY_CARD_VALID_FROM, -1);
            setValidFrom(tmpValidFrom != -1 ? new Date(tmpValidFrom) : null);
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_EXPIRY)) {
            long tmpExpiry = bundle.getLong(BUNDLE_LOYALTY_CARD_EXPIRY, -1);
            setExpiry(tmpExpiry != -1 ? new Date(tmpExpiry) : null);
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_BALANCE)) {
            setBalance(new BigDecimal(bundle.getString(BUNDLE_LOYALTY_CARD_BALANCE)));
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_BALANCE_TYPE)) {
            String tmpBalanceType = bundle.getString(BUNDLE_LOYALTY_CARD_BALANCE_TYPE, null);
            setBalanceType(tmpBalanceType != null ? Currency.getInstance(tmpBalanceType) : null);
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_CARD_ID)) {
            setCardId(Objects.requireNonNull(bundle.getString(BUNDLE_LOYALTY_CARD_CARD_ID)));
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_BARCODE_ID)) {
            setBarcodeId(bundle.getString(BUNDLE_LOYALTY_CARD_BARCODE_ID));
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_BARCODE_TYPE)) {
            String tmpBarcodeType = bundle.getString(BUNDLE_LOYALTY_CARD_BARCODE_TYPE, null);
            setBarcodeType(tmpBarcodeType != null ? CatimaBarcode.fromName(tmpBarcodeType) : null);
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_HEADER_COLOR)) {
            int tmpHeaderColor = bundle.getInt(BUNDLE_LOYALTY_CARD_HEADER_COLOR, -1);
            setHeaderColor(tmpHeaderColor != -1 ? tmpHeaderColor : null);
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_STAR_STATUS)) {
            setStarStatus(bundle.getInt(BUNDLE_LOYALTY_CARD_STAR_STATUS));
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_LAST_USED)) {
            setLastUsed(bundle.getLong(BUNDLE_LOYALTY_CARD_LAST_USED));
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_ZOOM_LEVEL)) {
            setZoomLevel(bundle.getInt(BUNDLE_LOYALTY_CARD_ZOOM_LEVEL));
        }
        if (bundle.containsKey(BUNDLE_LOYALTY_CARD_ARCHIVE_STATUS)) {
            setArchiveStatus(bundle.getInt(BUNDLE_LOYALTY_CARD_ARCHIVE_STATUS));
        }
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();

        bundle.putInt(BUNDLE_LOYALTY_CARD_ID, id);
        bundle.putString(BUNDLE_LOYALTY_CARD_STORE, store);
        bundle.putString(BUNDLE_LOYALTY_CARD_NOTE, note);
        if (validFrom != null) {
            bundle.putLong(BUNDLE_LOYALTY_CARD_VALID_FROM, validFrom.getTime());
        }
        if (expiry != null) {
            bundle.putLong(BUNDLE_LOYALTY_CARD_EXPIRY, expiry.getTime());
        }
        bundle.putString(BUNDLE_LOYALTY_CARD_BALANCE, balance.toString());
        if (balanceType != null) {
            bundle.putString(BUNDLE_LOYALTY_CARD_BALANCE_TYPE, balanceType.toString());
        }
        bundle.putString(BUNDLE_LOYALTY_CARD_CARD_ID, cardId);
        bundle.putString(BUNDLE_LOYALTY_CARD_BARCODE_ID, barcodeId);
        if (barcodeType != null) {
            bundle.putString(BUNDLE_LOYALTY_CARD_BARCODE_TYPE, barcodeType.name());
        }
        if (headerColor != null) {
            bundle.putInt(BUNDLE_LOYALTY_CARD_HEADER_COLOR, headerColor);
        }
        bundle.putInt(BUNDLE_LOYALTY_CARD_STAR_STATUS, starStatus);
        bundle.putLong(BUNDLE_LOYALTY_CARD_LAST_USED, lastUsed);
        bundle.putInt(BUNDLE_LOYALTY_CARD_ZOOM_LEVEL, zoomLevel);
        bundle.putInt(BUNDLE_LOYALTY_CARD_ARCHIVE_STATUS, archiveStatus);

        return bundle;
    }

    public static LoyaltyCard fromCursor(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.ID));
        String store = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.STORE));
        String note = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.NOTE));
        long validFromLong = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.VALID_FROM));
        long expiryLong = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.EXPIRY));
        BigDecimal balance = new BigDecimal(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BALANCE)));
        String cardId = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.CARD_ID));
        String barcodeId = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BARCODE_ID));
        int starStatus = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.STAR_STATUS));
        long lastUsed = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.LAST_USED));
        int zoomLevel = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.ZOOM_LEVEL));
        int archiveStatus = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.ARCHIVE_STATUS));

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

        return new LoyaltyCard(id, store, note, validFrom, expiry, balance, balanceType, cardId, barcodeId, barcodeType, headerColor, starStatus, lastUsed, zoomLevel, archiveStatus);
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
        return 0;
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
