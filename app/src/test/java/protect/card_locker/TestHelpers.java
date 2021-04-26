package protect.card_locker;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;

public class TestHelpers {
    static public DBHelper getEmptyDb(Activity activity) {
        DBHelper db = new DBHelper(activity);
        // Make sure DB is empty
        SQLiteDatabase database = db.getWritableDatabase();
        database.execSQL("delete from " + DBHelper.LoyaltyCardDbIds.TABLE);
        database.execSQL("delete from " + DBHelper.LoyaltyCardDbGroups.TABLE);
        database.execSQL("delete from " + DBHelper.LoyaltyCardDbIdsGroups.TABLE);
        database.close();

        return db;
    }
}
