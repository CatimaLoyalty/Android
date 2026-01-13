package protect.card_locker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.zxing.BarcodeFormat;

import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

public class TestHelpers {
    private static final String BARCODE_DATA = "428311627547";
    private static final CatimaBarcode BARCODE_TYPE = CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A);

    public static DBHelper getEmptyDb(Context context) {
        DBHelper db = new DBHelper(context);
        SQLiteDatabase database = db.getWritableDatabase();

        // Make sure no files remain
        Cursor cursor = DBHelper.getLoyaltyCardCursor(database);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            int cardID = cursor.getColumnIndex(DBHelper.LoyaltyCardDbIds.ID);

            for (ImageLocationType imageLocationType : ImageLocationType.values()) {
                try {
                    Utils.saveCardImage(context.getApplicationContext(), null, cardID, imageLocationType);
                } catch (FileNotFoundException ignored) {
                }
            }

            cursor.moveToNext();
        }

        // Make sure DB is empty
        database.execSQL("delete from " + DBHelper.LoyaltyCardDbIds.TABLE);
        database.execSQL("delete from " + DBHelper.LoyaltyCardDbGroups.TABLE);
        database.execSQL("delete from " + DBHelper.LoyaltyCardDbIdsGroups.TABLE);

        return db;
    }

    /**
     * Add the given number of cards, each with an index in the store name.
     *
     * @param mDatabase
     * @param cardsToAdd
     */
    public static void addLoyaltyCards(final SQLiteDatabase mDatabase, final int cardsToAdd) {
        // Add in reverse order to test sorting
        for (int index = cardsToAdd; index > 0; index--) {
            String storeName = String.format("store, \"%4d", index);
            String note = String.format("note, \"%4d", index);
            long id = DBHelper.insertLoyaltyCard(mDatabase, storeName, note, null, null, new BigDecimal(String.valueOf(index)), null, BARCODE_DATA, null, BARCODE_TYPE, StandardCharsets.ISO_8859_1, index, 0, null,0);
            boolean result = (id != -1);
            assertTrue(result);
        }

        assertEquals(cardsToAdd, DBHelper.getLoyaltyCardCount(mDatabase));
    }

    public static void addGroups(final SQLiteDatabase mDatabase, int groupsToAdd) {
        // Add in reverse order to test sorting
        for (int index = groupsToAdd; index > 0; index--) {
            String groupName = String.format("group, \"%4d", index);
            long id = DBHelper.insertGroup(mDatabase, groupName);
            boolean result = (id != -1);
            assertTrue(result);
        }

        assertEquals(groupsToAdd, DBHelper.getGroupCount(mDatabase));
    }
}
