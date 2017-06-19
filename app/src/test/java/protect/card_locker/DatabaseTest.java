package protect.card_locker;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.zxing.BarcodeFormat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23)
public class DatabaseTest
{
    private DBHelper db;

    @Before
    public void setUp()
    {
        Activity activity = Robolectric.setupActivity(MainActivity.class);
        db = new DBHelper(activity);
    }

    @Test
    public void addRemoveOneGiftCard()
    {
        assertEquals(0, db.getLoyaltyCardCount());
        boolean result = db.insertLoyaltyCard("store", "note", "cardId", BarcodeFormat.UPC_A.toString());
        assertTrue(result);
        assertEquals(1, db.getLoyaltyCardCount());

        LoyaltyCard loyaltyCard = db.getLoyaltyCard(1);
        assertNotNull(loyaltyCard);
        assertEquals("store", loyaltyCard.store);
        assertEquals("note", loyaltyCard.note);
        assertEquals("cardId", loyaltyCard.cardId);
        assertEquals(BarcodeFormat.UPC_A.toString(), loyaltyCard.barcodeType);

        result = db.deleteLoyaltyCard(1);
        assertTrue(result);
        assertEquals(0, db.getLoyaltyCardCount());
        assertNull(db.getLoyaltyCard(1));
    }

    @Test
    public void updateGiftCard()
    {
        boolean result = db.insertLoyaltyCard("store", "note", "cardId", BarcodeFormat.UPC_A.toString());
        assertTrue(result);
        assertEquals(1, db.getLoyaltyCardCount());

        result = db.updateLoyaltyCard(1, "store1", "note1", "cardId1", BarcodeFormat.AZTEC.toString());
        assertTrue(result);
        assertEquals(1, db.getLoyaltyCardCount());

        LoyaltyCard loyaltyCard = db.getLoyaltyCard(1);
        assertNotNull(loyaltyCard);
        assertEquals("store1", loyaltyCard.store);
        assertEquals("note1", loyaltyCard.note);
        assertEquals("cardId1", loyaltyCard.cardId);
        assertEquals(BarcodeFormat.AZTEC.toString(), loyaltyCard.barcodeType);
    }

    @Test
    public void updateMissingGiftCard()
    {
        assertEquals(0, db.getLoyaltyCardCount());

        boolean result = db.updateLoyaltyCard(1, "store1", "note1", "cardId1",
                BarcodeFormat.UPC_A.toString());
        assertEquals(false, result);
        assertEquals(0, db.getLoyaltyCardCount());
    }

    @Test
    public void emptyGiftCardValues()
    {
        boolean result = db.insertLoyaltyCard("", "", "", "");
        assertTrue(result);
        assertEquals(1, db.getLoyaltyCardCount());

        LoyaltyCard loyaltyCard = db.getLoyaltyCard(1);
        assertNotNull(loyaltyCard);
        assertEquals("", loyaltyCard.store);
        assertEquals("", loyaltyCard.note);
        assertEquals("", loyaltyCard.cardId);
        assertEquals("", loyaltyCard.barcodeType);
    }

    @Test
    public void giftCardsViaCursor()
    {
        final int CARDS_TO_ADD = 10;

        // Add the gift cards in reverse order, to ensure
        // that they are sorted
        for(int index = CARDS_TO_ADD-1; index >= 0; index--)
        {
            boolean result = db.insertLoyaltyCard("store" + index, "note" + index, "cardId" + index,
                    BarcodeFormat.UPC_A.toString());
            assertTrue(result);
        }

        assertEquals(CARDS_TO_ADD, db.getLoyaltyCardCount());

        Cursor cursor = db.getLoyaltyCardCursor();
        assertNotNull(cursor);

        assertEquals(CARDS_TO_ADD, cursor.getCount());

        cursor.moveToFirst();

        for(int index = 0; index < CARDS_TO_ADD; index++)
        {
            assertEquals("store"+index, cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.STORE)));
            assertEquals("note"+index, cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.NOTE)));
            assertEquals("cardId"+index, cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.CARD_ID)));
            assertEquals(BarcodeFormat.UPC_A.toString(), cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BARCODE_TYPE)));

            cursor.moveToNext();
        }

        assertTrue(cursor.isAfterLast());
    }

    private void setupDatabaseVersion1(SQLiteDatabase database)
    {
        // Delete the tables as they exist now
        database.execSQL("drop table " + DBHelper.LoyaltyCardDbIds.TABLE);

        // Create the table as it existed in revision 1
        database.execSQL("create table " + DBHelper.LoyaltyCardDbIds.TABLE + "(" +
                DBHelper.LoyaltyCardDbIds.ID + " INTEGER primary key autoincrement," +
                DBHelper.LoyaltyCardDbIds.STORE + " TEXT not null," +
                DBHelper.LoyaltyCardDbIds.CARD_ID + " TEXT not null," +
                DBHelper.LoyaltyCardDbIds.BARCODE_TYPE + " TEXT not null)");
    }

    private int insertCardVersion1(SQLiteDatabase database,
                                    final String store, final String cardId,
                                    final String barcodeType)
    {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DBHelper.LoyaltyCardDbIds.STORE, store);
        contentValues.put(DBHelper.LoyaltyCardDbIds.CARD_ID, cardId);
        contentValues.put(DBHelper.LoyaltyCardDbIds.BARCODE_TYPE, barcodeType);
        final long newId = database.insert(DBHelper.LoyaltyCardDbIds.TABLE, null, contentValues);
        assertTrue(newId != -1);
        return (int)newId;
    }

    @Test
    public void databaseUpgradeFromVersion1()
    {
        SQLiteDatabase database = db.getWritableDatabase();

        // Setup the database as it appeared in revision 1
        setupDatabaseVersion1(database);

        // Insert a budget and transaction
        int newCardId = insertCardVersion1(database, "store", "cardId", BarcodeFormat.UPC_A.toString());

        // Upgrade database
        db.onUpgrade(database, DBHelper.ORIGINAL_DATABASE_VERSION, DBHelper.DATABASE_VERSION);

        // Determine that the entries are queryable and the fields are correct
        LoyaltyCard card = db.getLoyaltyCard(newCardId);
        assertEquals("store", card.store);
        assertEquals("cardId", card.cardId);
        assertEquals(BarcodeFormat.UPC_A.toString(), card.barcodeType);
        assertEquals("", card.note);

        database.close();
    }
}
