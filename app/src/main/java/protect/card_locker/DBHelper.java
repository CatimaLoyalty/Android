package protect.card_locker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.Date;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "Catima.db";
    public static final int ORIGINAL_DATABASE_VERSION = 1;
    public static final int DATABASE_VERSION = 16;

    public static class LoyaltyCardDbGroups {
        public static final String TABLE = "groups";
        public static final String ID = "_id";
        public static final String ORDER = "orderId";
    }

    public static class LoyaltyCardDbIds {
        public static final String TABLE = "cards";
        public static final String ID = "_id";
        public static final String STORE = "store";
        public static final String EXPIRY = "expiry";
        public static final String BALANCE = "balance";
        public static final String BALANCE_TYPE = "balancetype";
        public static final String NOTE = "note";
        public static final String HEADER_COLOR = "headercolor";
        public static final String HEADER_TEXT_COLOR = "headertextcolor";
        public static final String CARD_ID = "cardid";
        public static final String BARCODE_ID = "barcodeid";
        public static final String BARCODE_TYPE = "barcodetype";
        public static final String STAR_STATUS = "starstatus";
        public static final String LAST_USED = "lastused";
        public static final String ZOOM_LEVEL = "zoomlevel";
        public static final String ZOOM_WIDTH = "zoomwidth";
        public static final String ARCHIVE_STATUS = "archive";
    }

    public static class LoyaltyCardDbIdsGroups {
        public static final String TABLE = "cardsGroups";
        public static final String cardID = "cardId";
        public static final String groupID = "groupId";
    }

    public static class LoyaltyCardDbFTS {
        public static final String TABLE = "fts";
        public static final String ID = "rowid"; // This should NEVER be changed
        public static final String STORE = "store";
        public static final String NOTE = "note";
    }

    public enum LoyaltyCardOrder {
        Alpha,
        LastUsed,
        Expiry
    }

    public enum LoyaltyCardOrderDirection {
        Ascending,
        Descending
    }

    public enum LoyaltyCardArchiveFilter {
        All,
        Archived,
        Unarchived
    }

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // create table for card groups
        db.execSQL("CREATE TABLE " + LoyaltyCardDbGroups.TABLE + "(" +
                LoyaltyCardDbGroups.ID + " TEXT primary key not null," +
                LoyaltyCardDbGroups.ORDER + " INTEGER DEFAULT '0')");

        // create table for cards
        // Balance is TEXT and not REAL to be able to store a BigDecimal without precision loss
        db.execSQL("CREATE TABLE " + LoyaltyCardDbIds.TABLE + "(" +
                LoyaltyCardDbIds.ID + " INTEGER primary key autoincrement," +
                LoyaltyCardDbIds.STORE + " TEXT not null," +
                LoyaltyCardDbIds.NOTE + " TEXT not null," +
                LoyaltyCardDbIds.EXPIRY + " INTEGER," +
                LoyaltyCardDbIds.BALANCE + " TEXT not null DEFAULT '0'," +
                LoyaltyCardDbIds.BALANCE_TYPE + " TEXT," +
                LoyaltyCardDbIds.HEADER_COLOR + " INTEGER," +
                LoyaltyCardDbIds.CARD_ID + " TEXT not null," +
                LoyaltyCardDbIds.BARCODE_ID + " TEXT," +
                LoyaltyCardDbIds.BARCODE_TYPE + " TEXT," +
                LoyaltyCardDbIds.STAR_STATUS + " INTEGER DEFAULT '0'," +
                LoyaltyCardDbIds.LAST_USED + " INTEGER DEFAULT '0', " +
                LoyaltyCardDbIds.ZOOM_LEVEL + " INTEGER DEFAULT '100', " +
                LoyaltyCardDbIds.ZOOM_WIDTH + " INTEGER DEFAULT '100', " +
                LoyaltyCardDbIds.ARCHIVE_STATUS + " INTEGER DEFAULT '0' )");

        // create associative table for cards in groups
        db.execSQL("CREATE TABLE " + LoyaltyCardDbIdsGroups.TABLE + "(" +
                LoyaltyCardDbIdsGroups.cardID + " INTEGER," +
                LoyaltyCardDbIdsGroups.groupID + " TEXT," +
                "primary key (" + LoyaltyCardDbIdsGroups.cardID + "," + LoyaltyCardDbIdsGroups.groupID + "))");

        // create FTS search table
        db.execSQL("CREATE VIRTUAL TABLE " + LoyaltyCardDbFTS.TABLE + " USING fts4(" +
                LoyaltyCardDbFTS.STORE + ", " + LoyaltyCardDbFTS.NOTE + ", " +
                "tokenize=unicode61);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2 && newVersion >= 2) {
            db.execSQL("ALTER TABLE " + LoyaltyCardDbIds.TABLE
                    + " ADD COLUMN " + LoyaltyCardDbIds.NOTE + " TEXT not null default ''");
        }

        if (oldVersion < 3 && newVersion >= 3) {
            db.execSQL("ALTER TABLE " + LoyaltyCardDbIds.TABLE
                    + " ADD COLUMN " + LoyaltyCardDbIds.HEADER_COLOR + " INTEGER");
            db.execSQL("ALTER TABLE " + LoyaltyCardDbIds.TABLE
                    + " ADD COLUMN " + LoyaltyCardDbIds.HEADER_TEXT_COLOR + " INTEGER");
        }

        if (oldVersion < 4 && newVersion >= 4) {
            db.execSQL("ALTER TABLE " + LoyaltyCardDbIds.TABLE
                    + " ADD COLUMN " + LoyaltyCardDbIds.STAR_STATUS + " INTEGER DEFAULT '0'");
        }

        if (oldVersion < 5 && newVersion >= 5) {
            db.execSQL("CREATE TABLE " + LoyaltyCardDbGroups.TABLE + "(" +
                    LoyaltyCardDbGroups.ID + " TEXT primary key not null)");

            db.execSQL("CREATE TABLE " + LoyaltyCardDbIdsGroups.TABLE + "(" +
                    LoyaltyCardDbIdsGroups.cardID + " INTEGER," +
                    LoyaltyCardDbIdsGroups.groupID + " TEXT," +
                    "primary key (" + LoyaltyCardDbIdsGroups.cardID + "," + LoyaltyCardDbIdsGroups.groupID + "))");
        }

        if (oldVersion < 6 && newVersion >= 6) {
            db.execSQL("ALTER TABLE " + LoyaltyCardDbGroups.TABLE
                    + " ADD COLUMN " + LoyaltyCardDbGroups.ORDER + " INTEGER DEFAULT '0'");
        }

        if (oldVersion < 7 && newVersion >= 7) {
            db.execSQL("ALTER TABLE " + LoyaltyCardDbIds.TABLE
                    + " ADD COLUMN " + LoyaltyCardDbIds.EXPIRY + " INTEGER");
        }

        if (oldVersion < 8 && newVersion >= 8) {
            db.execSQL("ALTER TABLE " + LoyaltyCardDbIds.TABLE
                    + " ADD COLUMN " + LoyaltyCardDbIds.BALANCE + " TEXT not null DEFAULT '0'");
            db.execSQL("ALTER TABLE " + LoyaltyCardDbIds.TABLE
                    + " ADD COLUMN " + LoyaltyCardDbIds.BALANCE_TYPE + " TEXT");
        }

        if (oldVersion < 9 && newVersion >= 9) {
            db.execSQL("ALTER TABLE " + LoyaltyCardDbIds.TABLE
                    + " ADD COLUMN " + LoyaltyCardDbIds.BARCODE_ID + " TEXT");
        }

        if (oldVersion < 10 && newVersion >= 10) {
            // SQLite doesn't support modify column
            // So we need to create a temp column to make barcode type nullable
            // Let's drop header text colour too while we're at it
            // https://www.sqlite.org/faq.html#q11
            db.beginTransaction();

            db.execSQL("CREATE TEMPORARY TABLE tmp (" +
                    LoyaltyCardDbIds.ID + " INTEGER primary key autoincrement," +
                    LoyaltyCardDbIds.STORE + " TEXT not null," +
                    LoyaltyCardDbIds.NOTE + " TEXT not null," +
                    LoyaltyCardDbIds.EXPIRY + " INTEGER," +
                    LoyaltyCardDbIds.BALANCE + " TEXT not null DEFAULT '0'," +
                    LoyaltyCardDbIds.BALANCE_TYPE + " TEXT," +
                    LoyaltyCardDbIds.HEADER_COLOR + " INTEGER," +
                    LoyaltyCardDbIds.CARD_ID + " TEXT not null," +
                    LoyaltyCardDbIds.BARCODE_ID + " TEXT," +
                    LoyaltyCardDbIds.BARCODE_TYPE + " TEXT," +
                    LoyaltyCardDbIds.STAR_STATUS + " INTEGER DEFAULT '0' )");

            db.execSQL("INSERT INTO tmp (" +
                    LoyaltyCardDbIds.ID + " ," +
                    LoyaltyCardDbIds.STORE + " ," +
                    LoyaltyCardDbIds.NOTE + " ," +
                    LoyaltyCardDbIds.EXPIRY + " ," +
                    LoyaltyCardDbIds.BALANCE + " ," +
                    LoyaltyCardDbIds.BALANCE_TYPE + " ," +
                    LoyaltyCardDbIds.HEADER_COLOR + " ," +
                    LoyaltyCardDbIds.CARD_ID + " ," +
                    LoyaltyCardDbIds.BARCODE_ID + " ," +
                    LoyaltyCardDbIds.BARCODE_TYPE + " ," +
                    LoyaltyCardDbIds.STAR_STATUS + ")" +
                    " SELECT " +
                    LoyaltyCardDbIds.ID + " ," +
                    LoyaltyCardDbIds.STORE + " ," +
                    LoyaltyCardDbIds.NOTE + " ," +
                    LoyaltyCardDbIds.EXPIRY + " ," +
                    LoyaltyCardDbIds.BALANCE + " ," +
                    LoyaltyCardDbIds.BALANCE_TYPE + " ," +
                    LoyaltyCardDbIds.HEADER_COLOR + " ," +
                    LoyaltyCardDbIds.CARD_ID + " ," +
                    LoyaltyCardDbIds.BARCODE_ID + " ," +
                    " NULLIF(" + LoyaltyCardDbIds.BARCODE_TYPE + ",'') ," +
                    LoyaltyCardDbIds.STAR_STATUS +
                    " FROM " + LoyaltyCardDbIds.TABLE);

            db.execSQL("DROP TABLE " + LoyaltyCardDbIds.TABLE);

            db.execSQL("CREATE TABLE " + LoyaltyCardDbIds.TABLE + "(" +
                    LoyaltyCardDbIds.ID + " INTEGER primary key autoincrement," +
                    LoyaltyCardDbIds.STORE + " TEXT not null," +
                    LoyaltyCardDbIds.NOTE + " TEXT not null," +
                    LoyaltyCardDbIds.EXPIRY + " INTEGER," +
                    LoyaltyCardDbIds.BALANCE + " TEXT not null DEFAULT '0'," +
                    LoyaltyCardDbIds.BALANCE_TYPE + " TEXT," +
                    LoyaltyCardDbIds.HEADER_COLOR + " INTEGER," +
                    LoyaltyCardDbIds.CARD_ID + " TEXT not null," +
                    LoyaltyCardDbIds.BARCODE_ID + " TEXT," +
                    LoyaltyCardDbIds.BARCODE_TYPE + " TEXT," +
                    LoyaltyCardDbIds.STAR_STATUS + " INTEGER DEFAULT '0' )");

            db.execSQL("INSERT INTO " + LoyaltyCardDbIds.TABLE + "(" +
                    LoyaltyCardDbIds.ID + " ," +
                    LoyaltyCardDbIds.STORE + " ," +
                    LoyaltyCardDbIds.NOTE + " ," +
                    LoyaltyCardDbIds.EXPIRY + " ," +
                    LoyaltyCardDbIds.BALANCE + " ," +
                    LoyaltyCardDbIds.BALANCE_TYPE + " ," +
                    LoyaltyCardDbIds.HEADER_COLOR + " ," +
                    LoyaltyCardDbIds.CARD_ID + " ," +
                    LoyaltyCardDbIds.BARCODE_ID + " ," +
                    LoyaltyCardDbIds.BARCODE_TYPE + " ," +
                    LoyaltyCardDbIds.STAR_STATUS + ")" +
                    " SELECT " +
                    LoyaltyCardDbIds.ID + " ," +
                    LoyaltyCardDbIds.STORE + " ," +
                    LoyaltyCardDbIds.NOTE + " ," +
                    LoyaltyCardDbIds.EXPIRY + " ," +
                    LoyaltyCardDbIds.BALANCE + " ," +
                    LoyaltyCardDbIds.BALANCE_TYPE + " ," +
                    LoyaltyCardDbIds.HEADER_COLOR + " ," +
                    LoyaltyCardDbIds.CARD_ID + " ," +
                    LoyaltyCardDbIds.BARCODE_ID + " ," +
                    LoyaltyCardDbIds.BARCODE_TYPE + " ," +
                    LoyaltyCardDbIds.STAR_STATUS +
                    " FROM tmp");

            db.execSQL("DROP TABLE tmp");

            db.setTransactionSuccessful();
            db.endTransaction();
        }

        if (oldVersion < 11 && newVersion >= 11) {
            db.execSQL("ALTER TABLE " + LoyaltyCardDbIds.TABLE
                    + " ADD COLUMN " + LoyaltyCardDbIds.LAST_USED + " INTEGER DEFAULT '0'");
        }

        if (oldVersion < 12 && newVersion >= 12) {
            db.execSQL("CREATE VIRTUAL TABLE " + LoyaltyCardDbFTS.TABLE + " USING fts4(" +
                    LoyaltyCardDbFTS.STORE + ", " + LoyaltyCardDbFTS.NOTE + ", " +
                    "tokenize=unicode61);");

            Cursor cursor = db.rawQuery("SELECT * FROM " + LoyaltyCardDbIds.TABLE + ";", null, null);

            cursor.moveToFirst();

            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.ID));
                String store = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.STORE));
                String note = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.NOTE));
                insertFTS(db, id, store, note);
            }
        }

        if (oldVersion < 13 && newVersion >= 13) {
            db.execSQL("DELETE FROM " + LoyaltyCardDbFTS.TABLE + ";");

            Cursor cursor = db.rawQuery("SELECT * FROM " + LoyaltyCardDbIds.TABLE + ";", null, null);

            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.ID));
                String store = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.STORE));
                String note = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.NOTE));
                insertFTS(db, id, store, note);

                while (cursor.moveToNext()) {
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.ID));
                    store = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.STORE));
                    note = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.NOTE));
                    insertFTS(db, id, store, note);
                }
            }
            cursor.close();
        }

        if (oldVersion < 14 && newVersion >= 14) {
            db.execSQL("ALTER TABLE " + LoyaltyCardDbIds.TABLE
                    + " ADD COLUMN " + LoyaltyCardDbIds.ZOOM_LEVEL + " INTEGER DEFAULT '100' ");
        }

        if (oldVersion < 16 && newVersion >= 16) {
            db.execSQL("ALTER TABLE " + LoyaltyCardDbIds.TABLE
                    + " ADD COLUMN " + LoyaltyCardDbIds.ZOOM_WIDTH + " INTEGER DEFAULT '100' ");
        }

        if (oldVersion < 15 && newVersion >= 15) {
            db.execSQL("ALTER TABLE " + LoyaltyCardDbIds.TABLE
                    + " ADD COLUMN " + LoyaltyCardDbIds.ARCHIVE_STATUS + " INTEGER DEFAULT '0' ");
        }

    }

    private static ContentValues generateFTSContentValues(final int id, final String store, final String note) {
        // FTS on Android is severely limited and can only search for word starting with a certain string
        // So for each word, we grab every single substring
        // This makes it possible to find Décathlon by searching both de and cat, for example

        ContentValues ftsContentValues = new ContentValues();

        StringBuilder storeString = new StringBuilder();
        for (String word : store.split(" ")) {
            for (int i = 0; i < word.length(); i++) {
                storeString.append(word);
                storeString.append(" ");
                word = word.substring(1);
            }
        }

        StringBuilder noteString = new StringBuilder();
        for (String word : note.split(" ")) {
            for (int i = 0; i < word.length(); i++) {
                noteString.append(word);
                noteString.append(" ");
                word = word.substring(1);
            }
        }

        ftsContentValues.put(LoyaltyCardDbFTS.ID, id);
        ftsContentValues.put(LoyaltyCardDbFTS.STORE, storeString.toString());
        ftsContentValues.put(LoyaltyCardDbFTS.NOTE, noteString.toString());

        return ftsContentValues;
    }

    private static void insertFTS(final SQLiteDatabase db, final int id, final String store, final String note) {
        db.insert(LoyaltyCardDbFTS.TABLE, null, generateFTSContentValues(id, store, note));
    }

    private static void updateFTS(final SQLiteDatabase db, final int id, final String store, final String note) {
        db.update(LoyaltyCardDbFTS.TABLE, generateFTSContentValues(id, store, note),
                whereAttrs(LoyaltyCardDbFTS.ID), withArgs(id));
    }

    public static long insertLoyaltyCard(
            final SQLiteDatabase database, final String store, final String note, final Date expiry,
            final BigDecimal balance, final Currency balanceType, final String cardId,
            final String barcodeId, final CatimaBarcode barcodeType, final Integer headerColor,
            final int starStatus, final Long lastUsed, final int archiveStatus) {
        database.beginTransaction();

        // Card
        ContentValues contentValues = new ContentValues();
        contentValues.put(LoyaltyCardDbIds.STORE, store);
        contentValues.put(LoyaltyCardDbIds.NOTE, note);
        contentValues.put(LoyaltyCardDbIds.EXPIRY, expiry != null ? expiry.getTime() : null);
        contentValues.put(LoyaltyCardDbIds.BALANCE, balance.toString());
        contentValues.put(LoyaltyCardDbIds.BALANCE_TYPE, balanceType != null ? balanceType.getCurrencyCode() : null);
        contentValues.put(LoyaltyCardDbIds.CARD_ID, cardId);
        contentValues.put(LoyaltyCardDbIds.BARCODE_ID, barcodeId);
        contentValues.put(LoyaltyCardDbIds.BARCODE_TYPE, barcodeType != null ? barcodeType.name() : null);
        contentValues.put(LoyaltyCardDbIds.HEADER_COLOR, headerColor);
        contentValues.put(LoyaltyCardDbIds.STAR_STATUS, starStatus);
        contentValues.put(LoyaltyCardDbIds.LAST_USED, lastUsed != null ? lastUsed : Utils.getUnixTime());
        contentValues.put(LoyaltyCardDbIds.ARCHIVE_STATUS, archiveStatus);
        long id = database.insert(LoyaltyCardDbIds.TABLE, null, contentValues);

        // FTS
        insertFTS(database, (int) id, store, note);

        database.setTransactionSuccessful();
        database.endTransaction();

        return id;
    }

    public static long insertLoyaltyCard(
            final SQLiteDatabase database, final int id, final String store, final String note,
            final Date expiry, final BigDecimal balance, final Currency balanceType,
            final String cardId, final String barcodeId, final CatimaBarcode barcodeType,
            final Integer headerColor, final int starStatus, final Long lastUsed, final int archiveStatus) {
        database.beginTransaction();

        // Card
        ContentValues contentValues = new ContentValues();
        contentValues.put(LoyaltyCardDbIds.ID, id);
        contentValues.put(LoyaltyCardDbIds.STORE, store);
        contentValues.put(LoyaltyCardDbIds.NOTE, note);
        contentValues.put(LoyaltyCardDbIds.EXPIRY, expiry != null ? expiry.getTime() : null);
        contentValues.put(LoyaltyCardDbIds.BALANCE, balance.toString());
        contentValues.put(LoyaltyCardDbIds.BALANCE_TYPE, balanceType != null ? balanceType.getCurrencyCode() : null);
        contentValues.put(LoyaltyCardDbIds.CARD_ID, cardId);
        contentValues.put(LoyaltyCardDbIds.BARCODE_ID, barcodeId);
        contentValues.put(LoyaltyCardDbIds.BARCODE_TYPE, barcodeType != null ? barcodeType.name() : null);
        contentValues.put(LoyaltyCardDbIds.HEADER_COLOR, headerColor);
        contentValues.put(LoyaltyCardDbIds.STAR_STATUS, starStatus);
        contentValues.put(LoyaltyCardDbIds.LAST_USED, lastUsed != null ? lastUsed : Utils.getUnixTime());
        contentValues.put(LoyaltyCardDbIds.ARCHIVE_STATUS, archiveStatus);
        database.insert(LoyaltyCardDbIds.TABLE, null, contentValues);

        // FTS
        insertFTS(database, id, store, note);

        database.setTransactionSuccessful();
        database.endTransaction();

        return id;
    }

    public static boolean updateLoyaltyCard(
            SQLiteDatabase database, final int id, final String store, final String note,
            final Date expiry, final BigDecimal balance, final Currency balanceType,
            final String cardId, final String barcodeId, final CatimaBarcode barcodeType,
            final Integer headerColor, final int starStatus, final Long lastUsed, final int archiveStatus) {
        database.beginTransaction();

        // Card
        ContentValues contentValues = new ContentValues();
        contentValues.put(LoyaltyCardDbIds.STORE, store);
        contentValues.put(LoyaltyCardDbIds.NOTE, note);
        contentValues.put(LoyaltyCardDbIds.EXPIRY, expiry != null ? expiry.getTime() : null);
        contentValues.put(LoyaltyCardDbIds.BALANCE, balance.toString());
        contentValues.put(LoyaltyCardDbIds.BALANCE_TYPE, balanceType != null ? balanceType.getCurrencyCode() : null);
        contentValues.put(LoyaltyCardDbIds.CARD_ID, cardId);
        contentValues.put(LoyaltyCardDbIds.BARCODE_ID, barcodeId);
        contentValues.put(LoyaltyCardDbIds.BARCODE_TYPE, barcodeType != null ? barcodeType.name() : null);
        contentValues.put(LoyaltyCardDbIds.HEADER_COLOR, headerColor);
        contentValues.put(LoyaltyCardDbIds.STAR_STATUS, starStatus);
        contentValues.put(LoyaltyCardDbIds.LAST_USED, lastUsed != null ? lastUsed : Utils.getUnixTime());
        contentValues.put(LoyaltyCardDbIds.ARCHIVE_STATUS, archiveStatus);

        int rowsUpdated = database.update(LoyaltyCardDbIds.TABLE, contentValues,
                whereAttrs(LoyaltyCardDbIds.ID), withArgs(id));

        // FTS
        updateFTS(database, id, store, note);

        database.setTransactionSuccessful();
        database.endTransaction();

        return (rowsUpdated == 1);
    }

    public static boolean updateLoyaltyCardArchiveStatus(SQLiteDatabase database, final int id, final int archiveStatus) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(LoyaltyCardDbIds.ARCHIVE_STATUS, archiveStatus);
        int rowsUpdated = database.update(LoyaltyCardDbIds.TABLE, contentValues,
                whereAttrs(LoyaltyCardDbIds.ID),
                withArgs(id));
        return (rowsUpdated == 1);
    }

    public static boolean updateLoyaltyCardStarStatus(SQLiteDatabase database, final int id, final int starStatus) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(LoyaltyCardDbIds.STAR_STATUS, starStatus);
        int rowsUpdated = database.update(LoyaltyCardDbIds.TABLE, contentValues,
                whereAttrs(LoyaltyCardDbIds.ID),
                withArgs(id));
        return (rowsUpdated == 1);
    }

    public static boolean updateLoyaltyCardLastUsed(SQLiteDatabase database, final int id) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(LoyaltyCardDbIds.LAST_USED, System.currentTimeMillis() / 1000);
        int rowsUpdated = database.update(LoyaltyCardDbIds.TABLE, contentValues,
                whereAttrs(LoyaltyCardDbIds.ID),
                withArgs(id));
        return (rowsUpdated == 1);
    }

    public static boolean updateLoyaltyCardZoomLevel(SQLiteDatabase database, int loyaltyCardId, int zoomLevel) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(LoyaltyCardDbIds.ZOOM_LEVEL, zoomLevel);
        Log.d("updateLoyaltyCardZLevel", "Card Id = " + loyaltyCardId + " Zoom level= " + zoomLevel);
        int rowsUpdated = database.update(LoyaltyCardDbIds.TABLE, contentValues,
                whereAttrs(LoyaltyCardDbIds.ID),
                withArgs(loyaltyCardId));
        Log.d("updateLoyaltyCardZLevel", "Rows changed = " + rowsUpdated);
        return (rowsUpdated == 1);
    }

    /**
     * Updates the zoom width of a card.
     * @param database database where the card is located
     * @param loyaltyCardId id of the card
     * @param zoomWidth new zoom width of the card
     * @return whether exactly 1 row was updated
     */
    public static boolean updateLoyaltyCardZoomWidth(SQLiteDatabase database, int loyaltyCardId, int zoomWidth) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(LoyaltyCardDbIds.ZOOM_WIDTH, zoomWidth);
        Log.d("updateLoyaltyCardZWidth", "Card Id = " + loyaltyCardId + " Zoom width= " + zoomWidth);
        int rowsUpdated = database.update(LoyaltyCardDbIds.TABLE, contentValues,
                whereAttrs(LoyaltyCardDbIds.ID),
                withArgs(loyaltyCardId));
        Log.d("updateLoyaltyCardZWidth", "Rows changed = " + rowsUpdated);
        return (rowsUpdated == 1);
    }

    public static boolean updateLoyaltyCardBalance(SQLiteDatabase database, final int id, final BigDecimal newBalance) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(LoyaltyCardDbIds.BALANCE, newBalance.toString());
        int rowsUpdated = database.update(LoyaltyCardDbIds.TABLE, contentValues,
                whereAttrs(LoyaltyCardDbIds.ID),
                withArgs(id));
        return (rowsUpdated == 1);
    }

    public static LoyaltyCard getLoyaltyCard(SQLiteDatabase database, final int id) {
        Cursor data = database.query(LoyaltyCardDbIds.TABLE, null, whereAttrs(LoyaltyCardDbIds.ID), withArgs(id), null, null, null);

        LoyaltyCard card = null;

        if (data.getCount() == 1) {
            data.moveToFirst();
            card = LoyaltyCard.toLoyaltyCard(data);
        }

        data.close();

        return card;
    }

    public static List<Group> getLoyaltyCardGroups(SQLiteDatabase database, final int id) {
        Cursor data = database.rawQuery("select * from " + LoyaltyCardDbGroups.TABLE + " g " +
                " LEFT JOIN " + LoyaltyCardDbIdsGroups.TABLE + " ig ON ig." + LoyaltyCardDbIdsGroups.groupID + " = g." + LoyaltyCardDbGroups.ID +
                " where " + LoyaltyCardDbIdsGroups.cardID + "=?" +
                " ORDER BY " + LoyaltyCardDbIdsGroups.groupID, withArgs(id));

        List<Group> groups = new ArrayList<>();

        if (!data.moveToFirst()) {
            data.close();
            return groups;
        }

        groups.add(Group.toGroup(data));

        while (data.moveToNext()) {
            groups.add(Group.toGroup(data));
        }

        data.close();

        return groups;
    }

    public static void setLoyaltyCardGroups(SQLiteDatabase database, final int id, List<Group> groups) {
        // First delete lookup table entries associated with this card
        database.delete(LoyaltyCardDbIdsGroups.TABLE,
                whereAttrs(LoyaltyCardDbIdsGroups.cardID),
                withArgs(id));

        // Then create entries for selected values
        for (Group group : groups) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(LoyaltyCardDbIdsGroups.cardID, id);
            contentValues.put(LoyaltyCardDbIdsGroups.groupID, group._id);
            database.insert(LoyaltyCardDbIdsGroups.TABLE, null, contentValues);
        }
    }

    public static boolean deleteLoyaltyCard(SQLiteDatabase database, Context context, final int id) {
        // Delete card
        int rowsDeleted = database.delete(LoyaltyCardDbIds.TABLE,
                whereAttrs(LoyaltyCardDbIds.ID),
                withArgs(id));

        // And delete lookup table entries associated with this card
        database.delete(LoyaltyCardDbIdsGroups.TABLE,
                whereAttrs(LoyaltyCardDbIdsGroups.cardID),
                withArgs(id));

        // Delete FTS table entries
        database.delete(LoyaltyCardDbFTS.TABLE,
                whereAttrs(LoyaltyCardDbFTS.ID),
                withArgs(id));

        // Also wipe card images associated with this card
        for (ImageLocationType imageLocationType : ImageLocationType.values()) {
            try {
                Utils.saveCardImage(context, null, id, imageLocationType);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        return (rowsDeleted == 1);
    }

    public static int getArchivedCardsCount(SQLiteDatabase database) {
        return (int) DatabaseUtils.queryNumEntries(database, LoyaltyCardDbIds.TABLE,
                whereAttrs(LoyaltyCardDbIds.ARCHIVE_STATUS), withArgs(1));
    }

    public static int getArchivedCardsCount(SQLiteDatabase database, final String groupName) {
        Cursor data = database.rawQuery(
                "select * from " + LoyaltyCardDbIds.TABLE + " c " +
                        " LEFT JOIN " + LoyaltyCardDbIdsGroups.TABLE + " cg " +
                        " ON c." + LoyaltyCardDbIds.ID + " = cg." + LoyaltyCardDbIdsGroups.cardID +
                " where " + LoyaltyCardDbIds.ARCHIVE_STATUS + " = 1" +
                " AND " + LoyaltyCardDbIdsGroups.groupID + "= ?",
                withArgs(groupName)
        );

        int count = data.getCount();

        data.close();
        return count;
    }

    public static Cursor getLoyaltyCardCursor(SQLiteDatabase database) {
        // An empty string will match everything
        return getLoyaltyCardCursor(database, LoyaltyCardArchiveFilter.All);
    }

    public static Cursor getLoyaltyCardCursor(SQLiteDatabase database, LoyaltyCardArchiveFilter archiveFilter) {
        // An empty string will match everything
        return getLoyaltyCardCursor(database, "", archiveFilter);
    }

    /**
     * Returns a cursor to all loyalty cards with the filter text in either the store or note.
     *
     * @param filter
     * @return Cursor
     */
    public static Cursor getLoyaltyCardCursor(SQLiteDatabase database, final String filter, LoyaltyCardArchiveFilter archiveFilter) {
        return getLoyaltyCardCursor(database, filter, null, archiveFilter);
    }

    /**
     * Returns a cursor to all loyalty cards with the filter text in either the store or note in a certain group.
     *
     * @param filter
     * @param group
     * @return Cursor
     */
    public static Cursor getLoyaltyCardCursor(SQLiteDatabase database, final String filter, Group group, LoyaltyCardArchiveFilter archiveFilter) {
        return getLoyaltyCardCursor(database, filter, group, LoyaltyCardOrder.Alpha, LoyaltyCardOrderDirection.Ascending, archiveFilter);
    }

    /**
     * Returns a cursor to all loyalty cards with the filter text in either the store or note in a certain group sorted as requested.
     *
     * @param filter
     * @param group
     * @param order
     * @return Cursor
     */
    public static Cursor getLoyaltyCardCursor(SQLiteDatabase database, String filter, Group group, LoyaltyCardOrder order, LoyaltyCardOrderDirection direction, LoyaltyCardArchiveFilter archiveFilter) {
        StringBuilder groupFilter = new StringBuilder();
        String limitString = "";

        if (group != null) {
            List<Integer> allowedIds = getGroupCardIds(database, group._id);

            // Empty group
            if (!allowedIds.isEmpty()) {
                groupFilter.append("AND (");

                for (int i = 0; i < allowedIds.size(); i++) {
                    groupFilter.append(LoyaltyCardDbIds.TABLE + "." + LoyaltyCardDbIds.ID + " = ").append(allowedIds.get(i));
                    if (i != allowedIds.size() - 1) {
                        groupFilter.append(" OR ");
                    }
                }
                groupFilter.append(") ");
            } else {
                limitString = "LIMIT 0";
            }
        }

        String archiveFilterString = "";
        if (archiveFilter != LoyaltyCardArchiveFilter.All) {
            archiveFilterString = " AND " + LoyaltyCardDbIds.TABLE + "." + LoyaltyCardDbIds.ARCHIVE_STATUS + " = " + (archiveFilter.equals(LoyaltyCardArchiveFilter.Unarchived) ? 0 : 1);
        }

        String orderField = getFieldForOrder(order);

        return database.rawQuery("SELECT " + LoyaltyCardDbIds.TABLE + ".* FROM " + LoyaltyCardDbIds.TABLE +
                " JOIN " + LoyaltyCardDbFTS.TABLE +
                " ON " + LoyaltyCardDbFTS.TABLE + "." + LoyaltyCardDbFTS.ID + " = " + LoyaltyCardDbIds.TABLE + "." + LoyaltyCardDbIds.ID +
                (filter.trim().isEmpty() ? " " : " AND " + LoyaltyCardDbFTS.TABLE + " MATCH ? ") +
                groupFilter.toString() +
                archiveFilterString +
                " ORDER BY " + LoyaltyCardDbIds.TABLE + "." + LoyaltyCardDbIds.ARCHIVE_STATUS + " ASC, " +
                LoyaltyCardDbIds.TABLE + "." + LoyaltyCardDbIds.STAR_STATUS + " DESC, " +
                " (CASE WHEN " + LoyaltyCardDbIds.TABLE + "." + orderField + " IS NULL THEN 1 ELSE 0 END), " +
                LoyaltyCardDbIds.TABLE + "." + orderField + " COLLATE NOCASE " + getDbDirection(order, direction) + ", " +
                LoyaltyCardDbIds.TABLE + "." + LoyaltyCardDbIds.STORE + " COLLATE NOCASE ASC " +
                limitString, filter.trim().isEmpty() ? null : new String[]{TextUtils.join("* ", filter.split(" ")) + '*'}, null);
    }

    /**
     * Returns the amount of loyalty cards.
     *
     * @return Integer
     */
    public static int getLoyaltyCardCount(SQLiteDatabase database) {
        return (int) DatabaseUtils.queryNumEntries(database, LoyaltyCardDbIds.TABLE);
    }

    /**
     * Returns a cursor to all groups.
     *
     * @return Cursor
     */
    public static Cursor getGroupCursor(SQLiteDatabase database) {
        return database.rawQuery("select * from " + LoyaltyCardDbGroups.TABLE +
                " ORDER BY " + LoyaltyCardDbGroups.ORDER + " ASC," + LoyaltyCardDbGroups.ID + " COLLATE NOCASE ASC", null, null);
    }

    public static List<Group> getGroups(SQLiteDatabase database) {
        Cursor data = getGroupCursor(database);

        List<Group> groups = new ArrayList<>();

        if (!data.moveToFirst()) {
            data.close();
            return groups;
        }

        groups.add(Group.toGroup(data));
        while (data.moveToNext()) {
            groups.add(Group.toGroup(data));
        }

        data.close();
        return groups;
    }

    public static void reorderGroups(SQLiteDatabase database, final List<Group> groups) {
        Integer order = 0;

        for (Group group : groups) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(LoyaltyCardDbGroups.ORDER, order);

            database.update(LoyaltyCardDbGroups.TABLE, contentValues,
                    whereAttrs(LoyaltyCardDbGroups.ID),
                    withArgs(group._id));

            order++;
        }
    }

    public static Group getGroup(SQLiteDatabase database, final String groupName) {
        Cursor data = database.query(LoyaltyCardDbGroups.TABLE, null,
                whereAttrs(LoyaltyCardDbGroups.ID), withArgs(groupName), null, null, null);

        Group group = null;
        if (data.getCount() == 1) {
            data.moveToFirst();
            group = Group.toGroup(data);
        }
        data.close();

        return group;
    }

    public static int getGroupCount(SQLiteDatabase database) {
        return (int) DatabaseUtils.queryNumEntries(database, LoyaltyCardDbGroups.TABLE);
    }

    public static List<Integer> getGroupCardIds(SQLiteDatabase database, final String groupName) {
        Cursor data = database.query(LoyaltyCardDbIdsGroups.TABLE, withArgs(LoyaltyCardDbIdsGroups.cardID),
                whereAttrs(LoyaltyCardDbIdsGroups.groupID), withArgs(groupName), null, null, null);
        List<Integer> cardIds = new ArrayList<>();

        if (!data.moveToFirst()) {
            return cardIds;
        }

        cardIds.add(data.getInt(0));

        while (data.moveToNext()) {
            cardIds.add(data.getInt(0));
        }

        data.close();

        return cardIds;
    }

    public static long insertGroup(SQLiteDatabase database, final String name) {
        if (name.isEmpty()) return -1;

        ContentValues contentValues = new ContentValues();
        contentValues.put(LoyaltyCardDbGroups.ID, name);
        contentValues.put(LoyaltyCardDbGroups.ORDER, getGroupCount(database));
        return database.insert(LoyaltyCardDbGroups.TABLE, null, contentValues);
    }

    public static boolean updateGroup(SQLiteDatabase database, final String groupName, final String newName) {
        if (newName.isEmpty()) return false;

        boolean success = false;

        ContentValues groupContentValues = new ContentValues();
        groupContentValues.put(LoyaltyCardDbGroups.ID, newName);

        ContentValues lookupContentValues = new ContentValues();
        lookupContentValues.put(LoyaltyCardDbIdsGroups.groupID, newName);

        database.beginTransaction();
        try {
            // Update group name
            int groupsChanged = database.update(LoyaltyCardDbGroups.TABLE, groupContentValues,
                    whereAttrs(LoyaltyCardDbGroups.ID),
                    withArgs(groupName));

            // Also update lookup tables
            database.update(LoyaltyCardDbIdsGroups.TABLE, lookupContentValues,
                    whereAttrs(LoyaltyCardDbIdsGroups.groupID),
                    withArgs(groupName));

            if (groupsChanged == 1) {
                database.setTransactionSuccessful();
                success = true;
            }
        } catch (SQLiteException ignored) {
        } finally {
            database.endTransaction();
        }

        return success;
    }

    public static boolean deleteGroup(SQLiteDatabase database, final String groupName) {
        boolean success = false;

        database.beginTransaction();
        try {
            // Delete group
            int groupsDeleted = database.delete(LoyaltyCardDbGroups.TABLE,
                    whereAttrs(LoyaltyCardDbGroups.ID),
                    withArgs(groupName));

            // And delete lookup table entries associated with this group
            database.delete(LoyaltyCardDbIdsGroups.TABLE,
                    whereAttrs(LoyaltyCardDbIdsGroups.groupID),
                    withArgs(groupName));

            if (groupsDeleted == 1) {
                database.setTransactionSuccessful();
                success = true;
            }
        } finally {
            database.endTransaction();
        }

        // Reorder after delete to ensure no bad order IDs
        reorderGroups(database, getGroups(database));

        return success;
    }

    public static int getGroupCardCount(SQLiteDatabase database, final String groupName) {
        return (int) DatabaseUtils.queryNumEntries(database, LoyaltyCardDbIdsGroups.TABLE,
                whereAttrs(LoyaltyCardDbIdsGroups.groupID), withArgs(groupName));
    }

    static private String whereAttrs(String... attrs) {
        if (attrs.length == 0) {
            return null;
        }
        StringBuilder whereClause = new StringBuilder(attrs[0]).append("=?");
        for (int i = 1; i < attrs.length; i++) {
            whereClause.append(" AND ").append(attrs[i]).append("=?");
        }
        return whereClause.toString();
    }

    static private String[] withArgs(Object... object) {
        return Arrays.stream(object)
                .map(String::valueOf)
                .toArray(String[]::new);
    }

    private static String getFieldForOrder(LoyaltyCardOrder order) {
        if (order == LoyaltyCardOrder.Alpha) {
            return LoyaltyCardDbIds.STORE;
        }

        if (order == LoyaltyCardOrder.LastUsed) {
            return LoyaltyCardDbIds.LAST_USED;
        }

        if (order == LoyaltyCardOrder.Expiry) {
            return LoyaltyCardDbIds.EXPIRY;
        }

        throw new IllegalArgumentException("Unknown order " + order);
    }

    private static String getDbDirection(LoyaltyCardOrder order, LoyaltyCardOrderDirection direction) {
        if (order == LoyaltyCardOrder.LastUsed) {
            // We want the default sorting to put the most recently used first
            return direction == LoyaltyCardOrderDirection.Descending ? "ASC" : "DESC";
        }

        return direction == LoyaltyCardOrderDirection.Ascending ? "ASC" : "DESC";
    }

    public static int getColumnCount(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT * FROM " + LoyaltyCardDbIds.TABLE + ";", null, null);
        return cursor.getColumnCount();
    }
}
