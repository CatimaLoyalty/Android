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
    public static final int DATABASE_VERSION = 14;

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

    private Context mContext;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        mContext = context;
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
                LoyaltyCardDbIds.ZOOM_LEVEL + " INTEGER DEFAULT '100' )");

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
    }

    private ContentValues generateFTSContentValues(final int id, final String store, final String note) {
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

    private void insertFTS(final SQLiteDatabase db, final int id, final String store, final String note) {
        db.insert(LoyaltyCardDbFTS.TABLE, null, generateFTSContentValues(id, store, note));
    }

    private void updateFTS(final SQLiteDatabase db, final int id, final String store, final String note) {
        db.update(LoyaltyCardDbFTS.TABLE, generateFTSContentValues(id, store, note),
                whereAttrs(LoyaltyCardDbFTS.ID), withArgs(id));
    }

    public long insertLoyaltyCard(final String store, final String note, final Date expiry,
                                  final BigDecimal balance, final Currency balanceType,
                                  final String cardId, final String barcodeId,
                                  final CatimaBarcode barcodeType, final Integer headerColor,
                                  final int starStatus, final Long lastUsed) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

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
        long id = db.insert(LoyaltyCardDbIds.TABLE, null, contentValues);

        // FTS
        insertFTS(db, (int) id, store, note);

        db.setTransactionSuccessful();
        db.endTransaction();

        return id;
    }

    public long insertLoyaltyCard(final SQLiteDatabase db, final String store,
                                  final String note, final Date expiry, final BigDecimal balance,
                                  final Currency balanceType, final String cardId,
                                  final String barcodeId, final CatimaBarcode barcodeType,
                                  final Integer headerColor, final int starStatus,
                                  final Long lastUsed) {
        db.beginTransaction();

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
        long id = db.insert(LoyaltyCardDbIds.TABLE, null, contentValues);

        // FTS
        insertFTS(db, (int) id, store, note);

        db.setTransactionSuccessful();
        db.endTransaction();

        return id;
    }

    public long insertLoyaltyCard(final SQLiteDatabase db, final int id, final String store,
                                  final String note, final Date expiry, final BigDecimal balance,
                                  final Currency balanceType, final String cardId,
                                  final String barcodeId, final CatimaBarcode barcodeType,
                                  final Integer headerColor, final int starStatus,
                                  final Long lastUsed) {
        db.beginTransaction();

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
        db.insert(LoyaltyCardDbIds.TABLE, null, contentValues);

        // FTS
        insertFTS(db, id, store, note);

        db.setTransactionSuccessful();
        db.endTransaction();

        return id;
    }

    public boolean updateLoyaltyCard(final int id, final String store, final String note,
                                     final Date expiry, final BigDecimal balance,
                                     final Currency balanceType, final String cardId,
                                     final String barcodeId, final CatimaBarcode barcodeType,
                                     final Integer headerColor) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

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
        int rowsUpdated = db.update(LoyaltyCardDbIds.TABLE, contentValues,
                whereAttrs(LoyaltyCardDbIds.ID), withArgs(id));

        // FTS
        updateFTS(db, id, store, note);

        db.setTransactionSuccessful();
        db.endTransaction();

        return (rowsUpdated == 1);
    }

    public boolean updateLoyaltyCardStarStatus(final int id, final int starStatus) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(LoyaltyCardDbIds.STAR_STATUS, starStatus);
        int rowsUpdated = db.update(LoyaltyCardDbIds.TABLE, contentValues,
                whereAttrs(LoyaltyCardDbIds.ID),
                withArgs(id));
        return (rowsUpdated == 1);
    }

    public boolean updateLoyaltyCardLastUsed(final int id) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(LoyaltyCardDbIds.LAST_USED, System.currentTimeMillis() / 1000);
        int rowsUpdated = db.update(LoyaltyCardDbIds.TABLE, contentValues,
                whereAttrs(LoyaltyCardDbIds.ID),
                withArgs(id));
        return (rowsUpdated == 1);
    }

    public boolean updateLoyaltyCardZoomLevel(int loyaltyCardId, int zoomLevel) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(LoyaltyCardDbIds.ZOOM_LEVEL, zoomLevel);
        Log.d("updateLoyaltyCardZLevel", "Card Id = " + loyaltyCardId + " Zoom level= " + zoomLevel);
        int rowsUpdated = db.update(LoyaltyCardDbIds.TABLE, contentValues,
                whereAttrs(LoyaltyCardDbIds.ID),
                withArgs(loyaltyCardId));
        Log.d("updateLoyaltyCardZLevel", "Rows changed = " + rowsUpdated);
        return (rowsUpdated == 1);
    }

    public LoyaltyCard getLoyaltyCard(final int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor data = db.query(LoyaltyCardDbIds.TABLE, null, whereAttrs(LoyaltyCardDbIds.ID), withArgs(id), null, null, null);

        LoyaltyCard card = null;

        if (data.getCount() == 1) {
            data.moveToFirst();
            card = LoyaltyCard.toLoyaltyCard(data);
        }

        data.close();

        return card;
    }

    public List<Group> getLoyaltyCardGroups(final int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor data = db.rawQuery("select * from " + LoyaltyCardDbGroups.TABLE + " g " +
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

    public void setLoyaltyCardGroups(final int id, List<Group> groups) {
        SQLiteDatabase db = getWritableDatabase();

        // First delete lookup table entries associated with this card
        db.delete(LoyaltyCardDbIdsGroups.TABLE,
                whereAttrs(LoyaltyCardDbIdsGroups.cardID),
                withArgs(id));

        // Then create entries for selected values
        for (Group group : groups) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(LoyaltyCardDbIdsGroups.cardID, id);
            contentValues.put(LoyaltyCardDbIdsGroups.groupID, group._id);
            db.insert(LoyaltyCardDbIdsGroups.TABLE, null, contentValues);
        }
    }

    public void setLoyaltyCardGroups(final SQLiteDatabase db, final int id, List<Group> groups) {
        // First delete lookup table entries associated with this card
        db.delete(LoyaltyCardDbIdsGroups.TABLE,
                whereAttrs(LoyaltyCardDbIdsGroups.cardID),
                withArgs(id));

        // Then create entries for selected values
        for (Group group : groups) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(LoyaltyCardDbIdsGroups.cardID, id);
            contentValues.put(LoyaltyCardDbIdsGroups.groupID, group._id);
            db.insert(LoyaltyCardDbIdsGroups.TABLE, null, contentValues);
        }
    }

    public boolean deleteLoyaltyCard(final int id) {
        SQLiteDatabase db = getWritableDatabase();
        // Delete card
        int rowsDeleted = db.delete(LoyaltyCardDbIds.TABLE,
                whereAttrs(LoyaltyCardDbIds.ID),
                withArgs(id));

        // And delete lookup table entries associated with this card
        db.delete(LoyaltyCardDbIdsGroups.TABLE,
                whereAttrs(LoyaltyCardDbIdsGroups.cardID),
                withArgs(id));

        // Delete FTS table entries
        db.delete(LoyaltyCardDbFTS.TABLE,
                whereAttrs(LoyaltyCardDbFTS.ID),
                withArgs(id));

        // Also wipe card images associated with this card
        for (ImageLocationType imageLocationType : ImageLocationType.values()) {
            try {
                Utils.saveCardImage(mContext, null, id, imageLocationType);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        return (rowsDeleted == 1);
    }

    public Cursor getLoyaltyCardCursor() {
        // An empty string will match everything
        return getLoyaltyCardCursor("");
    }

    /**
     * Returns a cursor to all loyalty cards with the filter text in either the store or note.
     *
     * @param filter
     * @return Cursor
     */
    public Cursor getLoyaltyCardCursor(final String filter) {
        return getLoyaltyCardCursor(filter, null);
    }

    /**
     * Returns a cursor to all loyalty cards with the filter text in either the store or note in a certain group.
     *
     * @param filter
     * @param group
     * @return Cursor
     */
    public Cursor getLoyaltyCardCursor(final String filter, Group group) {
        return getLoyaltyCardCursor(filter, group, LoyaltyCardOrder.Alpha, LoyaltyCardOrderDirection.Ascending);
    }

    /**
     * Returns a cursor to all loyalty cards with the filter text in either the store or note in a certain group sorted as requested.
     *
     * @param filter
     * @param group
     * @param order
     * @return Cursor
     */
    public Cursor getLoyaltyCardCursor(String filter, Group group, LoyaltyCardOrder order, LoyaltyCardOrderDirection direction) {
        StringBuilder groupFilter = new StringBuilder();
        String limitString = "";

        SQLiteDatabase db = getReadableDatabase();

        if (group != null) {
            List<Integer> allowedIds = getGroupCardIds(group._id);

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

        String orderField = getFieldForOrder(order);

        return db.rawQuery("SELECT " + LoyaltyCardDbIds.TABLE + ".* FROM " + LoyaltyCardDbIds.TABLE +
                " JOIN " + LoyaltyCardDbFTS.TABLE +
                " ON " + LoyaltyCardDbFTS.TABLE + "." + LoyaltyCardDbFTS.ID + " = " + LoyaltyCardDbIds.TABLE + "." + LoyaltyCardDbIds.ID +
                (filter.trim().isEmpty() ? " " : " AND " + LoyaltyCardDbFTS.TABLE + " MATCH ? ") +
                groupFilter.toString() +
                " ORDER BY " + LoyaltyCardDbIds.TABLE + "." + LoyaltyCardDbIds.STAR_STATUS + " DESC, " +
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
    public int getLoyaltyCardCount() {
        SQLiteDatabase db = getReadableDatabase();
        return (int) DatabaseUtils.queryNumEntries(db, LoyaltyCardDbIds.TABLE);
    }

    /**
     * Returns a cursor to all groups.
     *
     * @return Cursor
     */
    public Cursor getGroupCursor() {
        SQLiteDatabase db = getReadableDatabase();

        return db.rawQuery("select * from " + LoyaltyCardDbGroups.TABLE +
                " ORDER BY " + LoyaltyCardDbGroups.ORDER + " ASC," + LoyaltyCardDbGroups.ID + " COLLATE NOCASE ASC", null, null);
    }

    public List<Group> getGroups() {
        Cursor data = getGroupCursor();

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

    public void reorderGroups(final List<Group> groups) {
        Integer order = 0;
        SQLiteDatabase db = getWritableDatabase();

        for (Group group : groups) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(LoyaltyCardDbGroups.ORDER, order);

            db.update(LoyaltyCardDbGroups.TABLE, contentValues,
                    whereAttrs(LoyaltyCardDbGroups.ID),
                    withArgs(group._id));

            order++;
        }
    }

    public Group getGroup(final String groupName) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor data = db.query(LoyaltyCardDbGroups.TABLE, null,
                whereAttrs(LoyaltyCardDbGroups.ID), withArgs(groupName), null, null, null);

        Group group = null;
        if (data.getCount() == 1) {
            data.moveToFirst();
            group = Group.toGroup(data);
        }
        data.close();

        return group;
    }

    public int getGroupCount() {
        SQLiteDatabase db = getReadableDatabase();
        return (int) DatabaseUtils.queryNumEntries(db, LoyaltyCardDbGroups.TABLE);
    }

    public List<Integer> getGroupCardIds(final String groupName) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor data = db.query(LoyaltyCardDbIdsGroups.TABLE, withArgs(LoyaltyCardDbIdsGroups.cardID),
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

    public long insertGroup(final String name) {
        if (name.isEmpty()) return -1;

        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(LoyaltyCardDbGroups.ID, name);
        contentValues.put(LoyaltyCardDbGroups.ORDER, getGroupCount());
        return db.insert(LoyaltyCardDbGroups.TABLE, null, contentValues);
    }

    public boolean insertGroup(final SQLiteDatabase db, final String name) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(LoyaltyCardDbGroups.ID, name);
        contentValues.put(LoyaltyCardDbGroups.ORDER, getGroupCount());
        final long newId = db.insert(LoyaltyCardDbGroups.TABLE, null, contentValues);
        return newId != -1;
    }

    public boolean updateGroup(final String groupName, final String newName) {
        if (newName.isEmpty()) return false;

        boolean success = false;

        SQLiteDatabase db = getWritableDatabase();
        ContentValues groupContentValues = new ContentValues();
        groupContentValues.put(LoyaltyCardDbGroups.ID, newName);

        ContentValues lookupContentValues = new ContentValues();
        lookupContentValues.put(LoyaltyCardDbIdsGroups.groupID, newName);

        db.beginTransaction();
        try {
            // Update group name
            int groupsChanged = db.update(LoyaltyCardDbGroups.TABLE, groupContentValues,
                    whereAttrs(LoyaltyCardDbGroups.ID),
                    withArgs(groupName));

            // Also update lookup tables
            db.update(LoyaltyCardDbIdsGroups.TABLE, lookupContentValues,
                    whereAttrs(LoyaltyCardDbIdsGroups.groupID),
                    withArgs(groupName));

            if (groupsChanged == 1) {
                db.setTransactionSuccessful();
                success = true;
            }
        } catch (SQLiteException e) {
        } finally {
            db.endTransaction();
        }

        return success;
    }

    public boolean deleteGroup(final String groupName) {
        boolean success = false;

        SQLiteDatabase db = getWritableDatabase();

        db.beginTransaction();
        try {
            // Delete group
            int groupsDeleted = db.delete(LoyaltyCardDbGroups.TABLE,
                    whereAttrs(LoyaltyCardDbGroups.ID),
                    withArgs(groupName));

            // And delete lookup table entries associated with this group
            db.delete(LoyaltyCardDbIdsGroups.TABLE,
                    whereAttrs(LoyaltyCardDbIdsGroups.groupID),
                    withArgs(groupName));

            if (groupsDeleted == 1) {
                db.setTransactionSuccessful();
                success = true;
            }
        } finally {
            db.endTransaction();
        }

        // Reorder after delete to ensure no bad order IDs
        reorderGroups(getGroups());

        return success;
    }

    public int getGroupCardCount(final String groupName) {
        SQLiteDatabase db = getReadableDatabase();

        return (int) DatabaseUtils.queryNumEntries(db, LoyaltyCardDbIdsGroups.TABLE,
                whereAttrs(LoyaltyCardDbIdsGroups.groupID), withArgs(groupName));
    }

    private String whereAttrs(String... attrs) {
        if (attrs.length == 0) {
            return null;
        }
        StringBuilder whereClause = new StringBuilder(attrs[0]).append("=?");
        for (int i = 1; i < attrs.length; i++) {
            whereClause.append(" AND ").append(attrs[i]).append("=?");
        }
        return whereClause.toString();
    }

    private String[] withArgs(Object... object) {
        return Arrays.stream(object)
                .map(String::valueOf)
                .toArray(String[]::new);
    }

    private String getFieldForOrder(LoyaltyCardOrder order) {
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

    private String getDbDirection(LoyaltyCardOrder order, LoyaltyCardOrderDirection direction) {
        if (order == LoyaltyCardOrder.LastUsed) {
            // We want the default sorting to put the most recently used first
            return direction == LoyaltyCardOrderDirection.Descending ? "ASC" : "DESC";
        }

        return direction == LoyaltyCardOrderDirection.Ascending ? "ASC" : "DESC";
    }
}
