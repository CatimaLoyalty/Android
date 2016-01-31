package protect.card_locker;

import android.app.Activity;
import android.database.Cursor;

import com.google.zxing.BarcodeFormat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 17)
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
        boolean result = db.insertLoyaltyCard("store", "cardId", BarcodeFormat.UPC_A.toString());
        assertTrue(result);
        assertEquals(1, db.getLoyaltyCardCount());

        LoyaltyCard loyaltyCard = db.getLoyaltyCard(1);
        assertNotNull(loyaltyCard);
        assertEquals("store", loyaltyCard.store);
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
        boolean result = db.insertLoyaltyCard("store", "cardId", BarcodeFormat.UPC_A.toString());
        assertTrue(result);
        assertEquals(1, db.getLoyaltyCardCount());

        result = db.updateLoyaltyCard(1, "store1", "cardId1", BarcodeFormat.AZTEC.toString());
        assertTrue(result);
        assertEquals(1, db.getLoyaltyCardCount());

        LoyaltyCard loyaltyCard = db.getLoyaltyCard(1);
        assertNotNull(loyaltyCard);
        assertEquals("store1", loyaltyCard.store);
        assertEquals("cardId1", loyaltyCard.cardId);
        assertEquals(BarcodeFormat.AZTEC.toString(), loyaltyCard.barcodeType);
    }

    @Test
    public void updateMissingGiftCard()
    {
        assertEquals(0, db.getLoyaltyCardCount());

        boolean result = db.updateLoyaltyCard(1, "store1", "cardId1", BarcodeFormat.UPC_A.toString());
        assertEquals(false, result);
        assertEquals(0, db.getLoyaltyCardCount());
    }

    @Test
    public void emptyGiftCardValues()
    {
        boolean result = db.insertLoyaltyCard("", "", "");
        assertTrue(result);
        assertEquals(1, db.getLoyaltyCardCount());

        LoyaltyCard loyaltyCard = db.getLoyaltyCard(1);
        assertNotNull(loyaltyCard);
        assertEquals("", loyaltyCard.store);
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
            boolean result = db.insertLoyaltyCard("store" + index, "cardId" + index, BarcodeFormat.UPC_A.toString());
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
            assertEquals("cardId"+index, cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.CARD_ID)));
            assertEquals(BarcodeFormat.UPC_A.toString(), cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BARCODE_TYPE)));

            cursor.moveToNext();
        }

        assertTrue(cursor.isAfterLast());
    }
}
