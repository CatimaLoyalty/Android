package protect.card_locker;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;

import com.google.zxing.BarcodeFormat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.io.InvalidObjectException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class ImportURITest {
    private ImportURIHelper importURIHelper;
    private SQLiteDatabase mDatabase;

    @Before
    public void setUp() {
        Activity activity = Robolectric.setupActivity(MainActivity.class);
        importURIHelper = new ImportURIHelper(activity);
        mDatabase = TestHelpers.getEmptyDb(activity).getWritableDatabase();
    }

    @Test
    public void ensureNoDataLoss() throws InvalidObjectException, UnsupportedEncodingException {
        // Generate card
        Date date = new Date();

        DBHelper.insertLoyaltyCard(mDatabase, "store", "This note contains evil symbols like & and = that will break the parser if not escaped right $#!%()*+;:á", date, date, new BigDecimal("100"), null, BarcodeFormat.UPC_E.toString(), BarcodeFormat.UPC_A.toString(), CatimaBarcode.fromBarcode(BarcodeFormat.QR_CODE), Color.BLACK, 1, null,0);

        // Get card
        LoyaltyCard card = DBHelper.getLoyaltyCard(mDatabase, 1);

        // Card to URI
        Uri cardUri = importURIHelper.toUri(card);

        // Parse URI
        LoyaltyCard parsedCard = importURIHelper.parse(cardUri);

        // Compare everything
        assertEquals(card.store, parsedCard.store);
        assertEquals(card.note, parsedCard.note);
        assertEquals(card.validFrom, parsedCard.validFrom);
        assertEquals(card.expiry, parsedCard.expiry);
        assertEquals(card.balance, parsedCard.balance);
        assertEquals(card.balanceType, parsedCard.balanceType);
        assertEquals(card.cardId, parsedCard.cardId);
        assertEquals(card.barcodeId, parsedCard.barcodeId);
        assertEquals(card.barcodeType.format(), parsedCard.barcodeType.format());
        assertEquals(card.headerColor, parsedCard.headerColor);
        // No export of starStatus for export URL foreseen therefore 0 will be imported
        assertEquals(0, parsedCard.starStatus);
        assertEquals(0, parsedCard.archiveStatus);
    }

    @Test
    public void ensureNoCrashOnMissingHeaderFields() throws InvalidObjectException, UnsupportedEncodingException {
        // Generate card
        DBHelper.insertLoyaltyCard(mDatabase, "store", "note", null, null, new BigDecimal("10.00"), Currency.getInstance("EUR"), BarcodeFormat.UPC_A.toString(), null, CatimaBarcode.fromBarcode(BarcodeFormat.QR_CODE), null, 0, null,0);

        // Get card
        LoyaltyCard card = DBHelper.getLoyaltyCard(mDatabase, 1);

        // Card to URI
        Uri cardUri = importURIHelper.toUri(card);

        // Parse URI
        LoyaltyCard parsedCard = importURIHelper.parse(cardUri);

        // Compare everything
        assertEquals(card.store, parsedCard.store);
        assertEquals(card.note, parsedCard.note);
        assertEquals(card.validFrom, parsedCard.validFrom);
        assertEquals(card.expiry, parsedCard.expiry);
        assertEquals(card.balance, parsedCard.balance);
        assertEquals(card.balanceType, parsedCard.balanceType);
        assertEquals(card.cardId, parsedCard.cardId);
        assertEquals(card.barcodeId, parsedCard.barcodeId);
        assertEquals(card.barcodeType.format(), parsedCard.barcodeType.format());
        assertNull(parsedCard.headerColor);
        // No export of starStatus for export URL foreseen therefore 0 will be imported
        assertEquals(0, parsedCard.starStatus);
        assertEquals(0, parsedCard.archiveStatus);
    }

    @Test
    public void failToParseInvalidUri() {
        try {
            importURIHelper.parse(Uri.parse("https://example.com/test"));
            assertTrue(false); // Shouldn't get here
        } catch (InvalidObjectException ex) {
            // Desired behaviour
        }
    }

    @Test
    public void failToParseBadData() {
        String[] urls = new String[4];
        urls[0] = "https://brarcher.github.io/loyalty-card-locker/share?stare=store&note=note&cardid=12345&barcodetype=ITF&headercolor=-416706";
        urls[1] = "https://thelastproject.github.io/Catima/share#stare%3Dstore%26note%3Dnote%26balance%3D0%26cardid%3D12345%26barcodetype%3DITF%26headercolor%3D-416706";
        urls[2] = "https://catima.app/share#stare%3Dstore%26note%3Dnote%26balance%3D0%26cardid%3D12345%26barcodetype%3DITF%26headercolor%3D-416706";
        urls[3] = "https://catima.app/share#";

        for (String url : urls) {
            try {
                //"stare" instead of store
                importURIHelper.parse(Uri.parse(url));
                assertTrue(false); // Shouldn't get here
            } catch (InvalidObjectException ex) {
                // Desired behaviour
            }
        }
    }

    @Test
    public void parseAdditionalUnforeseenData() {
        String[] urls = new String[3];
        urls[0] = "https://brarcher.github.io/loyalty-card-locker/share?store=store&note=note&cardid=12345&barcodetype=ITF&headercolor=-416706&headertextcolor=-1&notforeseen=no";
        urls[1] = "https://thelastproject.github.io/Catima/share#store%3Dstore%26note%3Dnote%26balance%3D0%26cardid%3D12345%26barcodetype%3DITF%26headercolor%3D-416706%26notforeseen%3Dno";
        urls[2] = "https://catima.app/share#store%3Dstore%26note%3Dnote%26balance%3D0%26cardid%3D12345%26barcodetype%3DITF%26headercolor%3D-416706%26notforeseen%3Dno";

        for (String url : urls) {
            LoyaltyCard parsedCard = null;
            try {
                parsedCard = importURIHelper.parse(Uri.parse(url));
            } catch (InvalidObjectException e) {
                e.printStackTrace();
            }

            // Compare everything
            assertEquals("store", parsedCard.store);
            assertEquals("note", parsedCard.note);
            assertEquals(null, parsedCard.validFrom);
            assertEquals(null, parsedCard.expiry);
            assertEquals(new BigDecimal("0"), parsedCard.balance);
            assertEquals(null, parsedCard.balanceType);
            assertEquals("12345", parsedCard.cardId);
            assertEquals(null, parsedCard.barcodeId);
            assertEquals(BarcodeFormat.ITF, parsedCard.barcodeType.format());
            assertEquals(Integer.valueOf(-416706), parsedCard.headerColor);
            assertEquals(0, parsedCard.starStatus);
        }
    }
}
