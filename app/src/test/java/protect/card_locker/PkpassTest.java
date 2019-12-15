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
import org.robolectric.shadows.ShadowContentResolver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23)
public class PkpassTest {
    private PkpassImporter pkpassImporter;

    @Before
    public void setUp()
    {
        Activity activity = Robolectric.setupActivity(MainActivity.class);
        pkpassImporter = new PkpassImporter(activity);
    }

    @Test
    public void parseGenericExample() throws IOException, JSONException
    {
        // https://github.com/keefmoon/Passbook-Example-Code/blob/master/Pass-Example-Generic/Pass-Example-Generic.pkpass
        InputStream inputStream = getClass().getResourceAsStream("Pass-Example-Generic.pkpass");

        LoyaltyCard card = pkpassImporter.fromInputStream(inputStream);

        // Compare everything
        assertEquals(BarcodeFormat.QR_CODE.name(), card.barcodeType);
        assertEquals("0000001", card.cardId);
        assertEquals("Staff Pass for Employee Number 001", card.note);
        assertEquals("Passbook Example Company", card.store);
        assertEquals(String.valueOf(Color.rgb(90, 90, 90)), card.headerColor.toString());
        assertEquals(String.valueOf(Color.rgb(255, 255, 255)), card.headerTextColor.toString());

        // Check if all the extras got parsed correctly
        ExtrasHelper extras = card.extras;
        assertEquals(7, extras.getAllValues("en").keySet().size());

        // Check all 7 values
        Iterator<Map.Entry<String, String>> extrasKeys = extras.getAllValues("en").entrySet().iterator();

        // 1
        Map.Entry<String, String> entry = extrasKeys.next();
        assertEquals("staffNumber", entry.getKey());
        assertEquals("001", entry.getValue());

        // 2
        entry = extrasKeys.next();
        assertEquals("staffName", entry.getKey());
        assertEquals("Peter Brooke", entry.getValue());

        // 3
        entry = extrasKeys.next();
        assertEquals("telephoneExt", entry.getKey());
        assertEquals("9779", entry.getValue());

        // 4
        entry = extrasKeys.next();
        assertEquals("jobTitle", entry.getKey());
        assertEquals("Chief Pass Creator", entry.getValue());

        // 5
        entry = extrasKeys.next();
        assertEquals("expiryDate", entry.getKey());
        assertEquals("2013-12-31T00:00-23:59", entry.getValue());

        // 6
        entry = extrasKeys.next();
        assertEquals("managersName", entry.getKey());
        assertEquals("Paul Bailey", entry.getValue());

        // 7
        entry = extrasKeys.next();
        assertEquals("managersExt", entry.getKey());
        assertEquals("9673", entry.getValue());
    }

    @Test
    public void parseEurowingsTicket() throws IOException, JSONException
    {
        // https://github.com/brarcher/loyalty-card-locker/issues/309#issuecomment-563465333
        InputStream inputStream = getClass().getResourceAsStream("Eurowings.pkpass");

        LoyaltyCard card = pkpassImporter.fromInputStream(inputStream);

        // Compare everything
        assertEquals(BarcodeFormat.AZTEC.name(), card.barcodeType);
        assertEquals("M1DOE/JOHN         JBZPPP CGNDBVEW 0954 251A016E0073 148>5181W 9250BEW 00000000000002A0000000000000 0                          N", card.cardId);
        assertEquals("Eurowings Boarding Pass", card.note);
        assertEquals("EUROWINGS", card.store);

        // Violates the spec, but we want to support it anyway...
        assertEquals(String.valueOf(Color.parseColor("#FFFFFF")), card.headerColor.toString());
        assertEquals(String.valueOf(Color.parseColor("#AA0061")), card.headerTextColor.toString());

        // Check if all the extras got parsed correctly
        ExtrasHelper extras = card.extras;
        assertEquals(18, extras.getAllValues("en").size());

        // Check all 18 values
        Iterator<Map.Entry<String, String>> extrasKeys = extras.getAllValues("en").entrySet().iterator();

        // 1
        Map.Entry<String, String> entry = extrasKeys.next();
        assertEquals("gate", entry.getKey());
        assertEquals("B61", entry.getValue());

        // 2
        entry = extrasKeys.next();
        assertEquals("seat", entry.getKey());
        assertEquals("16E", entry.getValue());

        // 3
        entry = extrasKeys.next();
        assertEquals("origin", entry.getKey());
        assertEquals("CGN", entry.getValue());

        // 4
        entry = extrasKeys.next();
        assertEquals("destination", entry.getKey());
        assertEquals("DBV", entry.getValue());

        // 5
        entry = extrasKeys.next();
        assertEquals("name", entry.getKey());
        assertEquals("John Doe", entry.getValue());

        // 6
        entry = extrasKeys.next();
        assertEquals("status", entry.getKey());
        assertEquals("-", entry.getValue());

        // 7
        entry = extrasKeys.next();
        assertEquals("boardinggroup", entry.getKey());
        assertEquals("GROUP 1", entry.getValue());

        // 8
        entry = extrasKeys.next();
        assertEquals("tarif", entry.getKey());
        assertEquals("SMART", entry.getValue());

        // 9
        entry = extrasKeys.next();
        assertEquals("flightNumber", entry.getKey());
        assertEquals("EW 954", entry.getValue());

        // 10
        entry = extrasKeys.next();
        assertEquals("departureDate", entry.getKey());
        assertEquals("08/09/2019", entry.getValue());

        // 11
        entry = extrasKeys.next();
        assertEquals("boarding", entry.getKey());
        assertEquals("05:00", entry.getValue());

        // 12
        entry = extrasKeys.next();
        assertEquals("closure", entry.getKey());
        assertEquals("05:15", entry.getValue());

        // 13
        entry = extrasKeys.next();
        assertEquals("info", entry.getKey());
        assertEquals("info_content_str", entry.getValue());

        // 14
        entry = extrasKeys.next();
        assertEquals("recordlocator", entry.getKey());
        assertEquals("JBZPPP", entry.getValue());

        // 15
        entry = extrasKeys.next();
        assertEquals("sequence", entry.getKey());
        assertEquals("73", entry.getValue());

        // 16
        entry = extrasKeys.next();
        assertEquals("notice", entry.getKey());
        assertEquals("notice_content_str", entry.getValue());

        // 17
        entry = extrasKeys.next();
        assertEquals("baggage", entry.getKey());
        assertEquals("baggage_content_str", entry.getValue());

        // 18
        entry = extrasKeys.next();
        assertEquals("contact", entry.getKey());
        assertEquals("contact_content_str", entry.getValue());
    }
}
