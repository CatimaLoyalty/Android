package protect.card_locker;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;

import com.google.zxing.BarcodeFormat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.InvalidObjectException;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class ImportURITest {
    private ImportURIHelper importURIHelper;
    private DBHelper db;

    @Before
    public void setUp()
    {
        Activity activity = Robolectric.setupActivity(MainActivity.class);
        importURIHelper = new ImportURIHelper(activity);
        db = TestHelpers.getEmptyDb(activity);
    }

    @Test
    public void ensureNoDataLoss() throws InvalidObjectException
    {
        // Generate card
        Date date = new Date();

        db.insertLoyaltyCard("store", "note", date, new BigDecimal("100"), null, BarcodeFormat.UPC_E.toString(), BarcodeFormat.UPC_A.toString(), BarcodeFormat.QR_CODE, Color.BLACK, 1, null, null);

        // Get card
        LoyaltyCard card = db.getLoyaltyCard(1);

        // Card to URI
        Uri cardUri = importURIHelper.toUri(card);

        // Parse URI
        LoyaltyCard parsedCard = importURIHelper.parse(cardUri);

        // Compare everything
        assertEquals(card.store, parsedCard.store);
        assertEquals(card.note, parsedCard.note);
        assertEquals(card.expiry, parsedCard.expiry);
        assertEquals(card.balance, parsedCard.balance);
        assertEquals(card.balanceType, parsedCard.balanceType);
        assertEquals(card.cardId, parsedCard.cardId);
        assertEquals(card.barcodeId, parsedCard.barcodeId);
        assertEquals(card.barcodeType, parsedCard.barcodeType);
        assertEquals(card.headerColor, parsedCard.headerColor);
        // No export of starStatus for export URL foreseen therefore 0 will be imported
        assertEquals(0, parsedCard.starStatus);
        // No export of front and back image for export URL foreseen therefore nothing will be imported
        assertEquals(null, parsedCard.frontImage);
        assertEquals(null, parsedCard.backImage);
    }

    @Test
    public void ensureNoCrashOnMissingHeaderFields() throws InvalidObjectException
    {
        // Generate card
        db.insertLoyaltyCard("store", "note", null, new BigDecimal("10.00"), Currency.getInstance("EUR"), BarcodeFormat.UPC_A.toString(), null, BarcodeFormat.QR_CODE, null, 0, null, null);

        // Get card
        LoyaltyCard card = db.getLoyaltyCard(1);

        // Card to URI
        Uri cardUri = importURIHelper.toUri(card);

        // Parse URI
        LoyaltyCard parsedCard = importURIHelper.parse(cardUri);

        // Compare everything
        assertEquals(card.store, parsedCard.store);
        assertEquals(card.note, parsedCard.note);
        assertEquals(card.expiry, parsedCard.expiry);
        assertEquals(card.balance, parsedCard.balance);
        assertEquals(card.balanceType, parsedCard.balanceType);
        assertEquals(card.cardId, parsedCard.cardId);
        assertEquals(card.barcodeId, parsedCard.barcodeId);
        assertEquals(card.barcodeType, parsedCard.barcodeType);
        assertNull(parsedCard.headerColor);
        // No export of starStatus for export URL foreseen therefore 0 will be imported
        assertEquals(0, parsedCard.starStatus);
        // No export of front and back image for export URL foreseen therefore nothing will be imported
        assertEquals(null, parsedCard.frontImage);
        assertEquals(null, parsedCard.backImage);
    }

    @Test
    public void failToParseInvalidUri()
    {
        try {
            importURIHelper.parse(Uri.parse("https://example.com/test"));
            assertTrue(false); // Shouldn't get here
        } catch(InvalidObjectException ex) {
            // Desired behaviour
        }
    }

    @Test
    public void failToParseBadData()
    {
        try {
            //"stare" instead of store
            importURIHelper.parse(Uri.parse("https://brarcher.github.io/loyalty-card-locker/share?stare=store&note=note&cardid=12345&barcodetype=ITF&headercolor=-416706"));
            assertTrue(false); // Shouldn't get here
        } catch(InvalidObjectException ex) {
            // Desired behaviour
        }
    }

    @Test
    public void parseAdditionalUnforeseenData()
    {
        LoyaltyCard parsedCard = null;
        try {
            parsedCard = importURIHelper.parse(Uri.parse("https://brarcher.github.io/loyalty-card-locker/share?store=store&note=note&cardid=12345&barcodetype=ITF&headercolor=-416706&headertextcolor=-1&notforeseen=no"));
        } catch (InvalidObjectException e) {
            e.printStackTrace();
        }

        // Compare everything
        assertEquals("store", parsedCard.store);
        assertEquals("note", parsedCard.note);
        assertEquals(null, parsedCard.expiry);
        assertEquals(new BigDecimal("0"), parsedCard.balance);
        assertEquals(null, parsedCard.balanceType);
        assertEquals("12345", parsedCard.cardId);
        assertEquals(null, parsedCard.barcodeId);
        assertEquals(BarcodeFormat.ITF, parsedCard.barcodeType);
        assertEquals(Integer.valueOf(-416706), parsedCard.headerColor);
        assertEquals(0, parsedCard.starStatus);
    }
}
