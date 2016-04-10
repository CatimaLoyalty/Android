package protect.card_locker;

import android.database.Cursor;

public class LoyaltyCard
{
    public final int id;
    public final String store;
    public final String note;
    public final String cardId;
    public final String barcodeType;

    public LoyaltyCard(final int id, final String store, final String note, final String cardId, final String barcodeType)
    {
        this.id = id;
        this.store = store;
        this.note = note;
        this.cardId = cardId;
        this.barcodeType = barcodeType;
    }

    public static LoyaltyCard toLoyaltyCard(Cursor cursor)
    {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.ID));
        String store = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.STORE));
        String note = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.NOTE));
        String cardId = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.CARD_ID));
        String barcodeType = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BARCODE_TYPE));

        return new LoyaltyCard(id, store, note, cardId, barcodeType);
    }
}
