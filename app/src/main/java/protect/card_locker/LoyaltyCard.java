package protect.card_locker;

import android.database.Cursor;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Date;

import androidx.annotation.Nullable;

public class LoyaltyCard
{
    public final int id;
    public final String store;
    public final String note;
    public final Date expiry;
    public final BigDecimal balance;
    public final Currency balanceType;
    public final String cardId;
    public final String barcodeType;

    @Nullable
    public final Integer headerColor;

    @Nullable
    public final Integer headerTextColor;

    public final int starStatus;

    public LoyaltyCard(final int id, final String store, final String note, final Date expiry,
                       final BigDecimal balance, final String balanceType, final String cardId,
                       final String barcodeType, final Integer headerColor, final Integer headerTextColor,
                       final int starStatus)
    {
        this.id = id;
        this.store = store;
        this.note = note;
        this.expiry = expiry;
        this.balance = balance;
        if (balanceType != null) {
            this.balanceType = Currency.getInstance(balanceType);
        } else {
            this.balanceType = null;
        }
        this.cardId = cardId;
        this.barcodeType = barcodeType;
        this.headerColor = headerColor;
        this.headerTextColor = headerTextColor;
        this.starStatus = starStatus;
    }

    public static LoyaltyCard toLoyaltyCard(Cursor cursor)
    {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.ID));
        String store = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.STORE));
        String note = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.NOTE));
        long expiryLong = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.EXPIRY));
        BigDecimal balance = new BigDecimal(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BALANCE)));
        String balanceType = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BALANCE_TYPE));
        String cardId = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.CARD_ID));
        String barcodeType = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BARCODE_TYPE));
        int starred = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.STAR_STATUS));

        int headerColorColumn = cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.HEADER_COLOR);
        int headerTextColorColumn = cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.HEADER_TEXT_COLOR);

        Date expiry = null;
        Integer headerColor = null;
        Integer headerTextColor = null;

        if(expiryLong > 0)
        {
            expiry = new Date(expiryLong);
        }

        if(cursor.isNull(headerColorColumn) == false)
        {
            headerColor = cursor.getInt(headerColorColumn);
        }

        if(cursor.isNull(headerTextColorColumn) == false)
        {
            headerTextColor = cursor.getInt(headerTextColorColumn);
        }

        return new LoyaltyCard(id, store, note, expiry, balance, balanceType, cardId, barcodeType, headerColor, headerTextColor, starred);
    }
}
