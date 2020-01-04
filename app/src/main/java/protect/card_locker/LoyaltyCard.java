package protect.card_locker;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

public class LoyaltyCard
{
    public final int id;
    public final String store;
    public final String note;
    public final String cardId;
    public final String barcodeType;

    @Nullable
    public final Integer headerColor;

    @Nullable
    public final Integer headerTextColor;

    @Nullable
    public final Bitmap icon;

    @Nullable
    public final ExtrasHelper extras;

    public LoyaltyCard(final int id, final String store, final String note, final String cardId,
                       final String barcodeType, final Integer headerColor, final Integer headerTextColor,
                       final Bitmap icon, final ExtrasHelper extras)
    {
        this.id = id;
        this.store = store;
        this.note = note;
        this.cardId = cardId;
        this.barcodeType = barcodeType;
        this.headerColor = headerColor;
        this.headerTextColor = headerTextColor;
        this.icon = icon;
        this.extras = extras;
    }

    public static LoyaltyCard toLoyaltyCard(Cursor cursor)
    {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.ID));
        String store = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.STORE));
        String note = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.NOTE));
        String cardId = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.CARD_ID));
        String barcodeType = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BARCODE_TYPE));

        int headerColorColumn = cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.HEADER_COLOR);
        int headerTextColorColumn = cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.HEADER_TEXT_COLOR);

        Integer headerColor = null;
        Integer headerTextColor = null;

        if(cursor.isNull(headerColorColumn) == false)
        {
            headerColor = cursor.getInt(headerColorColumn);
        }

        if(cursor.isNull(headerTextColorColumn) == false)
        {
            headerTextColor = cursor.getInt(headerTextColorColumn);
        }

        int iconColumn = cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.ICON);
        Bitmap icon = null;

        if(cursor.isNull(iconColumn) == false)
        {
            byte[] iconData = cursor.getBlob(iconColumn);
            icon = BitmapFactory.decodeByteArray(iconData, 0, iconData.length);
        }

        int extrasColumn = cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.EXTRAS);
        ExtrasHelper extras = new ExtrasHelper();

        if(cursor.isNull(extrasColumn) == false)
        {
            try
            {
                extras = extras.fromJSON(new JSONObject(cursor.getString(extrasColumn)));
            }
            catch (JSONException ex)
            {
                // That this is actually JSON is an implementation detail
                // The important part is that the DB is in a bad state
                throw new IllegalArgumentException(ex);
            }
        }

        return new LoyaltyCard(id, store, note, cardId, barcodeType, headerColor, headerTextColor, icon, extras);
    }
}
