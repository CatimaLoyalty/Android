package protect.card_locker;

import android.database.Cursor;

public class LoyaltyCard
{
    public final int id;
    public final String store;
    public final String cardId;
    public final String barcodeType;

    public LoyaltyCard(final int id, final String store, final String cardId, final String barcodeType)
    {
        this.id = id;
        this.store = store;
        this.cardId = cardId;
        this.barcodeType = barcodeType;
    }

    public static LoyaltyCard toLoyaltyCard(Cursor cursor)
    {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.ID));
        String store = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.STORE));
        String cardId = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.CARD_ID));
        String barcodeType = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BARCODE_TYPE));

        return new LoyaltyCard(id, store, cardId, barcodeType);
    }
}
