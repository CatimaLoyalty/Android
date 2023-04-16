package protect.card_locker;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class DatabaseTest {
    private SQLiteDatabase mDatabase;
    private Activity mActivity;

    private static final Integer DEFAULT_HEADER_COLOR = Color.BLACK;

    @Before
    public void setUp() {
        mActivity = Robolectric.setupActivity(MainActivity.class);
        mDatabase = TestHelpers.getEmptyDb(mActivity).getWritableDatabase();
    }

    @Test
    public void addRemoveOneGiftCard() {
        assertEquals(0, DBHelper.getLoyaltyCardCount(mDatabase));
        long id = DBHelper.insertLoyaltyCard(mDatabase, "store", "note", null, null, new BigDecimal("0"), null, "cardId", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), DEFAULT_HEADER_COLOR, 0, null,0);
        boolean result = (id != -1);
        assertTrue(result);
        assertEquals(1, DBHelper.getLoyaltyCardCount(mDatabase));

        LoyaltyCard loyaltyCard = DBHelper.getLoyaltyCard(mDatabase, 1);
        assertNotNull(loyaltyCard);
        assertEquals("store", loyaltyCard.store);
        assertEquals("note", loyaltyCard.note);
        assertEquals(null, loyaltyCard.validFrom);
        assertEquals(null, loyaltyCard.expiry);
        assertEquals(new BigDecimal("0"), loyaltyCard.balance);
        assertEquals(null, loyaltyCard.balanceType);
        assertEquals("cardId", loyaltyCard.cardId);
        assertEquals(null, loyaltyCard.barcodeId);
        assertEquals(BarcodeFormat.UPC_A, loyaltyCard.barcodeType.format());
        assertEquals(DEFAULT_HEADER_COLOR, loyaltyCard.headerColor);
        assertEquals(0, loyaltyCard.starStatus);
        assertEquals(0, loyaltyCard.archiveStatus);

        result = DBHelper.deleteLoyaltyCard(mDatabase, mActivity, 1);
        assertTrue(result);
        assertEquals(0, DBHelper.getLoyaltyCardCount(mDatabase));
        assertNull(DBHelper.getLoyaltyCard(mDatabase, 1));
    }

    @Test
    public void updateGiftCard() {
        long id = DBHelper.insertLoyaltyCard(mDatabase, "store", "note", null, null, new BigDecimal("0"), null, "cardId", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), DEFAULT_HEADER_COLOR, 0, null,0);
        boolean result = (id != -1);
        assertTrue(result);
        assertEquals(1, DBHelper.getLoyaltyCardCount(mDatabase));

        result = DBHelper.updateLoyaltyCard(mDatabase, 1, "store1", "note1", null, null, new BigDecimal("10.00"), Currency.getInstance("EUR"), "cardId1", null, CatimaBarcode.fromBarcode(BarcodeFormat.AZTEC), DEFAULT_HEADER_COLOR, 0, null, 0);
        assertTrue(result);
        assertEquals(1, DBHelper.getLoyaltyCardCount(mDatabase));

        LoyaltyCard loyaltyCard = DBHelper.getLoyaltyCard(mDatabase, 1);
        assertNotNull(loyaltyCard);
        assertEquals("store1", loyaltyCard.store);
        assertEquals("note1", loyaltyCard.note);
        assertEquals(null, loyaltyCard.validFrom);
        assertEquals(null, loyaltyCard.expiry);
        assertEquals(new BigDecimal("10.00"), loyaltyCard.balance);
        assertEquals(Currency.getInstance("EUR"), loyaltyCard.balanceType);
        assertEquals("cardId1", loyaltyCard.cardId);
        assertEquals(null, loyaltyCard.barcodeId);
        assertEquals(BarcodeFormat.AZTEC, loyaltyCard.barcodeType.format());
        assertEquals(DEFAULT_HEADER_COLOR, loyaltyCard.headerColor);
        assertEquals(0, loyaltyCard.starStatus);
        assertEquals(0, loyaltyCard.archiveStatus);
    }

    @Test
    public void updateGiftCardOnlyStar() {
        long id = DBHelper.insertLoyaltyCard(mDatabase, "store", "note", null, null, new BigDecimal("0"), null, "cardId", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), DEFAULT_HEADER_COLOR, 0, null,0);
        boolean result = (id != -1);
        assertTrue(result);
        assertEquals(1, DBHelper.getLoyaltyCardCount(mDatabase));

        result = DBHelper.updateLoyaltyCardStarStatus(mDatabase, 1, 1);
        assertTrue(result);
        assertEquals(1, DBHelper.getLoyaltyCardCount(mDatabase));

        LoyaltyCard loyaltyCard = DBHelper.getLoyaltyCard(mDatabase, 1);
        assertNotNull(loyaltyCard);
        assertEquals("store", loyaltyCard.store);
        assertEquals("note", loyaltyCard.note);
        assertEquals(null, loyaltyCard.validFrom);
        assertEquals(null, loyaltyCard.expiry);
        assertEquals(new BigDecimal("0"), loyaltyCard.balance);
        assertEquals(null, loyaltyCard.balanceType);
        assertEquals("cardId", loyaltyCard.cardId);
        assertEquals(null, loyaltyCard.barcodeId);
        assertEquals(BarcodeFormat.UPC_A, loyaltyCard.barcodeType.format());
        assertEquals(DEFAULT_HEADER_COLOR, loyaltyCard.headerColor);
        assertEquals(1, loyaltyCard.starStatus);
        assertEquals(0, loyaltyCard.archiveStatus);
    }

    @Test
    public void updateMissingGiftCard() {
        assertEquals(0, DBHelper.getLoyaltyCardCount(mDatabase));

        boolean result = DBHelper.updateLoyaltyCard(mDatabase, 1, "store1", "note1", null, null, new BigDecimal("0"), null, "cardId1",
                null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), DEFAULT_HEADER_COLOR, 0, null, 0);
        assertEquals(false, result);
        assertEquals(0, DBHelper.getLoyaltyCardCount(mDatabase));
    }

    @Test
    public void emptyGiftCardValues() {
        long id = DBHelper.insertLoyaltyCard(mDatabase, "", "", null, null, new BigDecimal("0"), null, "", null, null, null, 0, null,0);
        boolean result = (id != -1);
        assertTrue(result);
        assertEquals(1, DBHelper.getLoyaltyCardCount(mDatabase));

        LoyaltyCard loyaltyCard = DBHelper.getLoyaltyCard(mDatabase, 1);
        assertNotNull(loyaltyCard);
        assertEquals("", loyaltyCard.store);
        assertEquals("", loyaltyCard.note);
        assertEquals(null, loyaltyCard.validFrom);
        assertEquals(null, loyaltyCard.expiry);
        assertEquals(new BigDecimal("0"), loyaltyCard.balance);
        assertEquals(null, loyaltyCard.balanceType);
        assertEquals("", loyaltyCard.cardId);
        assertEquals(null, loyaltyCard.barcodeId);
        assertEquals(null, loyaltyCard.barcodeType);
        // headerColor is randomly generated when not given, so skip
        assertEquals(0, loyaltyCard.starStatus);
        assertEquals(0, loyaltyCard.archiveStatus);
    }

    @Test
    public void giftCardsViaCursor() {
        final int CARDS_TO_ADD = 10;

        // Add the gift cards in reverse order, to ensure
        // that they are sorted
        for (int index = CARDS_TO_ADD - 1; index >= 0; index--) {
            long id = DBHelper.insertLoyaltyCard(mDatabase, "store" + index, "note" + index, null, null, new BigDecimal("0"), null, "cardId" + index,
                    null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), index, 0, null,0);
            boolean result = (id != -1);
            assertTrue(result);
        }

        assertEquals(CARDS_TO_ADD, DBHelper.getLoyaltyCardCount(mDatabase));

        Cursor cursor = DBHelper.getLoyaltyCardCursor(mDatabase);
        assertNotNull(cursor);

        assertEquals(CARDS_TO_ADD, cursor.getCount());

        cursor.moveToFirst();

        for (int index = 0; index < CARDS_TO_ADD; index++) {
            assertEquals("store" + index, cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.STORE)));
            assertEquals("note" + index, cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.NOTE)));
            assertEquals(0, cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.VALID_FROM)));
            assertEquals(0, cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.EXPIRY)));
            assertEquals("0", cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BALANCE)));
            assertEquals(null, cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BALANCE_TYPE)));
            assertEquals("cardId" + index, cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.CARD_ID)));
            assertEquals(null, cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BARCODE_ID)));
            assertEquals(BarcodeFormat.UPC_A.toString(), cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BARCODE_TYPE)));
            assertEquals(index, cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.HEADER_COLOR)));
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.STAR_STATUS)));
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.ARCHIVE_STATUS)));

            cursor.moveToNext();
        }

        assertTrue(cursor.isAfterLast());

        cursor.close();
    }

    @Test
    public void giftCardsViaCursorWithOneStarred()      //sorting test; stared card should appear first
    {
        final int CARDS_TO_ADD = 10;
        long id;
        // Add the gift cards in reverse order and add one with STAR, to ensure
        // that they are sorted
        for (int index = CARDS_TO_ADD - 1; index >= 0; index--) {
            if (index == CARDS_TO_ADD - 1) {
                id = DBHelper.insertLoyaltyCard(mDatabase, "store" + index, "note" + index, null, null, new BigDecimal("0"), null, "cardId" + index,
                        null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), index, 1, null,0);
            } else {
                id = DBHelper.insertLoyaltyCard(mDatabase, "store" + index, "note" + index, null, null, new BigDecimal("0"), null, "cardId" + index,
                        null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), index, 0, null,0);
            }
            boolean result = (id != -1);
            assertTrue(result);
        }

        assertEquals(CARDS_TO_ADD, DBHelper.getLoyaltyCardCount(mDatabase));

        Cursor cursor = DBHelper.getLoyaltyCardCursor(mDatabase);
        assertNotNull(cursor);

        assertEquals(CARDS_TO_ADD, cursor.getCount());

        cursor.moveToFirst();
        int index = CARDS_TO_ADD - 1;
        assertEquals("store" + index, cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.STORE)));
        assertEquals("note" + index, cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.NOTE)));
        assertEquals(0, cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.VALID_FROM)));
        assertEquals(0, cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.EXPIRY)));
        assertEquals("0", cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BALANCE)));
        assertEquals(null, cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BALANCE_TYPE)));
        assertEquals("cardId" + index, cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.CARD_ID)));
        assertEquals(null, cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BARCODE_ID)));
        assertEquals(BarcodeFormat.UPC_A.toString(), cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BARCODE_TYPE)));
        assertEquals(index, cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.HEADER_COLOR)));
        assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.STAR_STATUS)));

        cursor.moveToNext();

        for (index = 0; index < CARDS_TO_ADD - 1; index++) {
            assertEquals("store" + index, cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.STORE)));
            assertEquals("note" + index, cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.NOTE)));
            assertEquals(null, cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.VALID_FROM)));
            assertEquals(null, cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.EXPIRY)));
            assertEquals("0", cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BALANCE)));
            assertEquals(null, cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BALANCE_TYPE)));
            assertEquals("cardId" + index, cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.CARD_ID)));
            assertEquals(null, cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BARCODE_ID)));
            assertEquals(BarcodeFormat.UPC_A.toString(), cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BARCODE_TYPE)));
            assertEquals(index, cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.HEADER_COLOR)));
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.STAR_STATUS)));

            cursor.moveToNext();
        }

        assertTrue(cursor.isAfterLast());

        cursor.close();
    }

    private void setupDatabaseVersion1(SQLiteDatabase database) {
        // Delete the tables as they exist now
        database.execSQL("drop table " + DBHelper.LoyaltyCardDbIds.TABLE);
        database.execSQL("drop table " + DBHelper.LoyaltyCardDbGroups.TABLE);
        database.execSQL("drop table " + DBHelper.LoyaltyCardDbIdsGroups.TABLE);
        database.execSQL("drop table " + DBHelper.LoyaltyCardDbFTS.TABLE);

        // Create the table as it existed in revision 1
        database.execSQL("create table " + DBHelper.LoyaltyCardDbIds.TABLE + "(" +
                DBHelper.LoyaltyCardDbIds.ID + " INTEGER primary key autoincrement," +
                DBHelper.LoyaltyCardDbIds.STORE + " TEXT not null," +
                DBHelper.LoyaltyCardDbIds.CARD_ID + " TEXT not null," +
                DBHelper.LoyaltyCardDbIds.BARCODE_TYPE + " TEXT not null)");
    }

    private int insertCardVersion1(SQLiteDatabase database,
                                   final String store, final String cardId,
                                   final String barcodeType) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DBHelper.LoyaltyCardDbIds.STORE, store);
        contentValues.put(DBHelper.LoyaltyCardDbIds.CARD_ID, cardId);
        contentValues.put(DBHelper.LoyaltyCardDbIds.BARCODE_TYPE, barcodeType);
        final long newId = database.insert(DBHelper.LoyaltyCardDbIds.TABLE, null, contentValues);
        assertTrue(newId != -1);
        return (int) newId;
    }

    @Test
    public void addRemoveOneGroup() {
        assertEquals(0, DBHelper.getGroupCount(mDatabase));
        long id = DBHelper.insertGroup(mDatabase, "group one");
        boolean result = (id != -1);
        assertTrue(result);
        assertEquals(1, DBHelper.getGroupCount(mDatabase));

        Group group = DBHelper.getGroup(mDatabase, "group one");
        assertNotNull(group);
        assertEquals("group one", group._id);

        result = DBHelper.deleteGroup(mDatabase, "group one");
        assertTrue(result);
        assertEquals(0, DBHelper.getGroupCount(mDatabase));
        assertNull(DBHelper.getGroup(mDatabase, "group one"));
    }

    @Test
    public void updateGroup() {
        // Create card
        assertEquals(0, DBHelper.getLoyaltyCardCount(mDatabase));
        long id = DBHelper.insertLoyaltyCard(mDatabase, "store", "note", null, null, new BigDecimal("0"), null, "cardId", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), DEFAULT_HEADER_COLOR, 0, null,0);
        boolean result = (id != -1);
        assertTrue(result);
        assertEquals(1, DBHelper.getLoyaltyCardCount(mDatabase));

        // Create group
        long groupId = DBHelper.insertGroup(mDatabase, "group one");
        result = (groupId != -1);
        assertTrue(result);
        assertEquals(1, DBHelper.getGroupCount(mDatabase));

        // Add card to group
        Group group = DBHelper.getGroup(mDatabase, "group one");
        List<Group> groupList1 = new ArrayList<>();
        groupList1.add(group);
        DBHelper.setLoyaltyCardGroups(mDatabase, 1, groupList1);

        // Ensure the card has one group and the group has one card
        List<Group> cardGroups = DBHelper.getLoyaltyCardGroups(mDatabase, (int) id);
        assertEquals(1, cardGroups.size());
        assertEquals("group one", cardGroups.get(0)._id);
        assertEquals(1, DBHelper.getGroupCardCount(mDatabase, "group one"));

        // Rename group
        result = DBHelper.updateGroup(mDatabase, "group one", "group one renamed");
        assertTrue(result);
        assertEquals(1, DBHelper.getGroupCount(mDatabase));

        // Group one no longer exists
        group = DBHelper.getGroup(mDatabase,"group one");
        assertNull(group);

        // But group one renamed does
        Group group2 = DBHelper.getGroup(mDatabase, "group one renamed");
        assertNotNull(group2);
        assertEquals("group one renamed", group2._id);

        // And card is in "group one renamed"
        // Ensure the card has one group and the group has one card
        cardGroups = DBHelper.getLoyaltyCardGroups(mDatabase, (int) id);
        assertEquals(1, cardGroups.size());
        assertEquals("group one renamed", cardGroups.get(0)._id);
        assertEquals(1, DBHelper.getGroupCardCount(mDatabase, "group one renamed"));
    }

    @Test
    public void updateMissingGroup() {
        assertEquals(0, DBHelper.getGroupCount(mDatabase));

        boolean result = DBHelper.updateGroup(mDatabase, "group one", "new name");
        assertEquals(false, result);
        assertEquals(0, DBHelper.getGroupCount(mDatabase));
    }

    @Test
    public void emptyGroupValues() {
        long id = DBHelper.insertGroup(mDatabase, "");
        boolean result = (id != -1);
        assertFalse(result);
        assertEquals(0, DBHelper.getLoyaltyCardCount(mDatabase));
    }

    @Test
    public void duplicateGroupName() {
        assertEquals(0, DBHelper.getGroupCount(mDatabase));
        long id = DBHelper.insertGroup(mDatabase, "group one");
        boolean result = (id != -1);
        assertTrue(result);
        assertEquals(1, DBHelper.getGroupCount(mDatabase));

        Group group = DBHelper.getGroup(mDatabase, "group one");
        assertNotNull(group);
        assertEquals("group one", group._id);

        // Should fail on duplicate
        long id2 = DBHelper.insertGroup(mDatabase, "group one");
        boolean result2 = (id2 != -1);
        assertFalse(result2);
        assertEquals(1, DBHelper.getGroupCount(mDatabase));
    }

    @Test
    public void updateGroupDuplicate() {
        long id = DBHelper.insertGroup(mDatabase, "group one");
        boolean result = (id != -1);
        assertTrue(result);
        assertEquals(1, DBHelper.getGroupCount(mDatabase));

        long id2 = DBHelper.insertGroup(mDatabase, "group two");
        boolean result2 = (id2 != -1);
        assertTrue(result2);
        assertEquals(2, DBHelper.getGroupCount(mDatabase));

        // Should fail when trying to rename group two to one
        boolean result3 = DBHelper.updateGroup(mDatabase, "group two", "group one");
        assertFalse(result3);
        assertEquals(2, DBHelper.getGroupCount(mDatabase));

        // Rename failed so both should still be the same
        Group group = DBHelper.getGroup(mDatabase, "group one");
        assertNotNull(group);
        assertEquals("group one", group._id);

        Group group2 = DBHelper.getGroup(mDatabase, "group two");
        assertNotNull(group2);
        assertEquals("group two", group2._id);
    }

    @Test
    public void cardAddAndRemoveGroups() {
        // Create card
        assertEquals(0, DBHelper.getLoyaltyCardCount(mDatabase));
        long id = DBHelper.insertLoyaltyCard(mDatabase, "store", "note", null, null, new BigDecimal("0"), null, "cardId", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), DEFAULT_HEADER_COLOR, 0, null,0);
        boolean result = (id != -1);
        assertTrue(result);
        assertEquals(1, DBHelper.getLoyaltyCardCount(mDatabase));

        // Create two groups to only one card
        assertEquals(0, DBHelper.getGroupCount(mDatabase));
        long gid = DBHelper.insertGroup(mDatabase, "one");
        boolean gresult = (gid != -1);
        assertTrue(gresult);

        long gid2 = DBHelper.insertGroup(mDatabase, "two");
        boolean gresult2 = (gid2 != -1);
        assertTrue(gresult2);

        assertEquals(2, DBHelper.getGroupCount(mDatabase));

        Group group1 = DBHelper.getGroup(mDatabase, "one");

        // Card has no groups by default
        List<Group> cardGroups = DBHelper.getLoyaltyCardGroups(mDatabase, 1);
        assertEquals(0, cardGroups.size());

        // Add one groups to card
        List<Group> groupList1 = new ArrayList<>();
        groupList1.add(group1);
        DBHelper.setLoyaltyCardGroups(mDatabase, 1, groupList1);

        List<Group> cardGroups1 = DBHelper.getLoyaltyCardGroups(mDatabase, 1);
        assertEquals(1, cardGroups1.size());
        assertEquals(cardGroups1.get(0)._id, group1._id);
        assertEquals(1, DBHelper.getGroupCardCount(mDatabase, "one"));
        assertEquals(0, DBHelper.getGroupCardCount(mDatabase, "two"));

        // Remove groups
        DBHelper.setLoyaltyCardGroups(mDatabase, 1, new ArrayList<Group>());
        List<Group> cardGroups2 = DBHelper.getLoyaltyCardGroups(mDatabase, 1);
        assertEquals(0, cardGroups2.size());
        assertEquals(0, DBHelper.getGroupCardCount(mDatabase, "one"));
        assertEquals(0, DBHelper.getGroupCardCount(mDatabase, "two"));
    }

    @Test
    public void databaseUpgradeFromVersion1() {
        DBHelper dbHelper = TestHelpers.getEmptyDb(mActivity);
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        // Setup the database as it appeared in revision 1
        setupDatabaseVersion1(database);

        // Insert a budget and transaction
        int newCardId = insertCardVersion1(database, "store", "cardId", BarcodeFormat.UPC_A.toString());
        int newCardId2 = insertCardVersion1(database, "store", "cardId", "");

        // Upgrade database
        dbHelper.onUpgrade(database, DBHelper.ORIGINAL_DATABASE_VERSION, DBHelper.DATABASE_VERSION);

        // Determine that the entries are queryable and the fields are correct
        LoyaltyCard card = DBHelper.getLoyaltyCard(database, newCardId);
        assertEquals("store", card.store);
        assertEquals("", card.note);
        assertEquals(null, card.validFrom);
        assertEquals(null, card.expiry);
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals("cardId", card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(BarcodeFormat.UPC_A, card.barcodeType.format());
        assertEquals(null, card.headerColor);
        assertEquals(0, card.starStatus);
        assertEquals(0, card.lastUsed);
        assertEquals(100, card.zoomLevel);

        // Determine that the entries are queryable and the fields are correct
        LoyaltyCard card2 = DBHelper.getLoyaltyCard(database, newCardId2);
        assertEquals("store", card2.store);
        assertEquals("", card2.note);
        assertEquals(null, card2.validFrom);
        assertEquals(null, card2.expiry);
        assertEquals(new BigDecimal("0"), card2.balance);
        assertEquals(null, card2.balanceType);
        assertEquals("cardId", card2.cardId);
        assertEquals(null, card2.barcodeId);
        assertEquals(null, card2.barcodeType); // Empty string should've become null
        assertEquals(null, card2.headerColor);
        assertEquals(0, card2.starStatus);
        assertEquals(0, card2.lastUsed);
        assertEquals(100, card2.zoomLevel);
    }

    @Test
    public void updateGiftCardOnlyBalance() {
        long id = DBHelper.insertLoyaltyCard(mDatabase, "store", "note", null, null, new BigDecimal("100"), null, "cardId", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), DEFAULT_HEADER_COLOR, 0, null,0);
        boolean result = (id != -1);
        assertTrue(result);
        assertEquals(1, DBHelper.getLoyaltyCardCount(mDatabase));

        result = DBHelper.updateLoyaltyCardBalance(mDatabase, 1, new BigDecimal(60));
        assertTrue(result);
        assertEquals(1, DBHelper.getLoyaltyCardCount(mDatabase));

        LoyaltyCard loyaltyCard = DBHelper.getLoyaltyCard(mDatabase, 1);
        assertNotNull(loyaltyCard);
        assertEquals("store", loyaltyCard.store);
        assertEquals("note", loyaltyCard.note);
        assertEquals(null, loyaltyCard.validFrom);
        assertEquals(null, loyaltyCard.expiry);
        assertEquals(new BigDecimal(60), loyaltyCard.balance);
        assertEquals(null, loyaltyCard.balanceType);
        assertEquals("cardId", loyaltyCard.cardId);
        assertEquals(null, loyaltyCard.barcodeId);
        assertEquals(BarcodeFormat.UPC_A, loyaltyCard.barcodeType.format());
        assertEquals(DEFAULT_HEADER_COLOR, loyaltyCard.headerColor);
        assertEquals(0, loyaltyCard.starStatus);
        assertEquals(0, loyaltyCard.archiveStatus);
    }
}
