package protect.card_locker;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import com.google.zxing.BarcodeFormat;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.io.InvalidObjectException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static protect.card_locker.DBHelper.LoyaltyCardDbIds;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23)
public class ImportURITest {
    private ImportURIHelper importURIHelper;
    private DBHelper db;

    @Before
    public void setUp()
    {
        Activity activity = Robolectric.setupActivity(MainActivity.class);
        importURIHelper = new ImportURIHelper(activity);
        db = new DBHelper(activity);
    }

    @Test
    public void ensureNoDataLoss() throws InvalidObjectException, JSONException
    {
        // Generate card
        db.insertLoyaltyCard("store", "note", BarcodeFormat.UPC_A.toString(), LoyaltyCardDbIds.BARCODE_TYPE, Color.BLACK, Color.WHITE, new JSONObject("{\"key\": \"value\"}"));

        // Get card
        LoyaltyCard card = db.getLoyaltyCard(1);

        // Card to URI
        Uri cardUri = importURIHelper.toUri(card);

        // Parse URI
        LoyaltyCard parsedCard = importURIHelper.parse(cardUri);

        // Compare everything
        assertEquals(card.barcodeType, parsedCard.barcodeType);
        assertEquals(card.cardId, parsedCard.cardId);
        assertEquals(card.headerColor, parsedCard.headerColor);
        assertEquals(card.headerTextColor, parsedCard.headerTextColor);
        assertEquals(card.note, parsedCard.note);
        assertEquals(card.store, parsedCard.store);
        assertEquals(card.extras.toString(), parsedCard.extras.toString());
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
}
