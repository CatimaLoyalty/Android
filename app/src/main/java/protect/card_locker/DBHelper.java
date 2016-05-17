package protect.card_locker;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper
{
    public static final String DATABASE_NAME = "LoyaltyCards.db";
    public static final int ORIGINAL_DATABASE_VERSION = 1;
    public static final int DATABASE_VERSION = 2;

    static class LoyaltyCardDbIds
    {
        public static final String TABLE = "cards";
        public static final String ID = "_id";
        public static final String STORE = "store";
        public static final String NOTE = "note";
        public static final String CARD_ID = "cardid";
        public static final String BARCODE_TYPE = "barcodetype";
    }

    public DBHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        // create table for gift cards
        db.execSQL("create table " + LoyaltyCardDbIds.TABLE + "(" +
                LoyaltyCardDbIds.ID + " INTEGER primary key autoincrement," +
                LoyaltyCardDbIds.STORE + " TEXT not null," +
                LoyaltyCardDbIds.NOTE + " TEXT not null," +
                LoyaltyCardDbIds.CARD_ID + " TEXT not null," +
                LoyaltyCardDbIds.BARCODE_TYPE + " TEXT not null)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        // Upgrade from version 1 to version 2
        if(oldVersion < 2 && newVersion >= 2)
        {
            db.execSQL("ALTER TABLE " + LoyaltyCardDbIds.TABLE
                    + " ADD COLUMN " + LoyaltyCardDbIds.NOTE + " TEXT not null default ''");
        }
    }

    public boolean insertLoyaltyCard(final String store, final String note, final String cardId,
                                     final String barcodeType)
    {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(LoyaltyCardDbIds.STORE, store);
        contentValues.put(LoyaltyCardDbIds.NOTE, note);
        contentValues.put(LoyaltyCardDbIds.CARD_ID, cardId);
        contentValues.put(LoyaltyCardDbIds.BARCODE_TYPE, barcodeType);
        final long newId = db.insert(LoyaltyCardDbIds.TABLE, null, contentValues);
        return (newId != -1);
    }

    public boolean insertLoyaltyCard(final SQLiteDatabase db, final int id,
                                     final String store, final String note, final String cardId,
                                     final String barcodeType)
    {
        ContentValues contentValues = new ContentValues();
        contentValues.put(LoyaltyCardDbIds.ID, id);
        contentValues.put(LoyaltyCardDbIds.STORE, store);
        contentValues.put(LoyaltyCardDbIds.NOTE, note);
        contentValues.put(LoyaltyCardDbIds.CARD_ID, cardId);
        contentValues.put(LoyaltyCardDbIds.BARCODE_TYPE, barcodeType);
        final long newId = db.insert(LoyaltyCardDbIds.TABLE, null, contentValues);
        return (newId != -1);
    }


    public boolean updateLoyaltyCard(final int id, final String store, final String note,
                                     final String cardId, final String barcodeType)
    {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(LoyaltyCardDbIds.STORE, store);
        contentValues.put(LoyaltyCardDbIds.NOTE, note);
        contentValues.put(LoyaltyCardDbIds.CARD_ID, cardId);
        contentValues.put(LoyaltyCardDbIds.BARCODE_TYPE, barcodeType);
        int rowsUpdated = db.update(LoyaltyCardDbIds.TABLE, contentValues,
                LoyaltyCardDbIds.ID + "=?",
                new String[]{Integer.toString(id)});
        return (rowsUpdated == 1);
    }

    public LoyaltyCard getLoyaltyCard(final int id)
    {
        SQLiteDatabase db = getReadableDatabase();
        Cursor data = db.rawQuery("select * from " + LoyaltyCardDbIds.TABLE +
                " where " + LoyaltyCardDbIds.ID + "=?", new String[]{String.format("%d", id)});

        LoyaltyCard card = null;

        if(data.getCount() == 1)
        {
            data.moveToFirst();
            card = LoyaltyCard.toLoyaltyCard(data);
        }

        data.close();

        return card;
    }

    public boolean deleteLoyaltyCard (final int id)
    {
        SQLiteDatabase db = getWritableDatabase();
        int rowsDeleted =  db.delete(LoyaltyCardDbIds.TABLE,
                LoyaltyCardDbIds.ID + " = ? ",
                new String[]{String.format("%d", id)});
        return (rowsDeleted == 1);
    }

    public Cursor getLoyaltyCardCursor()
    {
        SQLiteDatabase db = getReadableDatabase();
        Cursor res =  db.rawQuery("select * from " + LoyaltyCardDbIds.TABLE +
                " ORDER BY " + LoyaltyCardDbIds.STORE, null);
        return res;
    }

    public int getLoyaltyCardCount()
    {
        SQLiteDatabase db = getReadableDatabase();
        Cursor data =  db.rawQuery("SELECT Count(*) FROM " + LoyaltyCardDbIds.TABLE, null);

        int numItems = 0;

        if(data.getCount() == 1)
        {
            data.moveToFirst();
            numItems = data.getInt(0);
        }

        data.close();

        return numItems;
    }
}

