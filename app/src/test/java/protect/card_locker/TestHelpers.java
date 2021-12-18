package protect.card_locker;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.FileNotFoundException;

public class TestHelpers {
    static public DBHelper getEmptyDb(Activity activity) {
        DBHelper db = new DBHelper(activity);
        SQLiteDatabase database = db.getWritableDatabase();

        // Make sure no files remain
        Cursor cursor = DBHelper.getLoyaltyCardCursor(database);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            int cardID = cursor.getColumnIndex(DBHelper.LoyaltyCardDbIds.ID);

            for (ImageLocationType imageLocationType : ImageLocationType.values()) {
                try {
                    Utils.saveCardImage(activity.getApplicationContext(), null, cardID, imageLocationType);
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
}
