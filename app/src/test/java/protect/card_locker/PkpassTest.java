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
        JSONObject extras = card.extras;
        assertEquals(7, extras.length());

        // Check all 7 values
        Iterator<String> extrasKeys = extras.keys();

        // 1
        assertEquals("staffNumber", extrasKeys.next());
        assertEquals("001", extras.get("staffNumber"));

        // 2
        assertEquals("staffName", extrasKeys.next());
        assertEquals("Peter Brooke", extras.get("staffName"));

        // 3
        assertEquals("telephoneExt", extrasKeys.next());
        assertEquals("9779", extras.get("telephoneExt"));

        // 4
        assertEquals("jobTitle", extrasKeys.next());
        assertEquals("Chief Pass Creator", extras.get("jobTitle"));

        // 5
        assertEquals("expiryDate", extrasKeys.next());
        assertEquals("2013-12-31T00:00-23:59", extras.get("expiryDate"));

        // 6
        assertEquals("managersName", extrasKeys.next());
        assertEquals("Paul Bailey", extras.get("managersName"));

        // 7
        assertEquals("managersExt", extrasKeys.next());
        assertEquals("9673", extras.get("managersExt"));
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
        JSONObject extras = card.extras;
        assertEquals(18, extras.length());

        // Check all 18 values
        Iterator<String> extrasKeys = extras.keys();

        // 1
        assertEquals("gate", extrasKeys.next());
        assertEquals("B61", extras.get("gate"));

        // 2
        assertEquals("seat", extrasKeys.next());
        assertEquals("16E", extras.get("seat"));

        // 3
        assertEquals("origin", extrasKeys.next());
        assertEquals("CGN", extras.get("origin"));

        // 4
        assertEquals("destination", extrasKeys.next());
        assertEquals("DBV", extras.get("destination"));

        // 5
        assertEquals("name", extrasKeys.next());
        assertEquals("John Doe", extras.get("name"));

        // 6
        assertEquals("status", extrasKeys.next());
        assertEquals("-", extras.get("status"));

        // 7
        assertEquals("boardinggroup", extrasKeys.next());
        assertEquals("GROUP 1", extras.get("boardinggroup"));

        // 8
        assertEquals("tarif", extrasKeys.next());
        assertEquals("SMART", extras.get("tarif"));

        // 9
        assertEquals("flightNumber", extrasKeys.next());
        assertEquals("EW 954", extras.get("flightNumber"));

        // 10
        assertEquals("departureDate", extrasKeys.next());
        assertEquals("08/09/2019", extras.get("departureDate"));

        // 11
        assertEquals("boarding", extrasKeys.next());
        assertEquals("05:00", extras.get("boarding"));

        // 12
        assertEquals("closure", extrasKeys.next());
        assertEquals("05:15", extras.get("closure"));

        // 13
        assertEquals("info", extrasKeys.next());
        assertEquals("info_content_str", extras.get("info"));

        // 14
        assertEquals("recordlocator", extrasKeys.next());
        assertEquals("JBZPPP", extras.get("recordlocator"));

        // 15
        assertEquals("sequence", extrasKeys.next());
        assertEquals("73", extras.get("sequence"));

        // 16
        assertEquals("notice", extrasKeys.next());
        assertEquals("notice_content_str", extras.get("notice"));

        // 17
        assertEquals("baggage", extrasKeys.next());
        assertEquals("baggage_content_str", extras.get("baggage"));

        // 18
        assertEquals("contact", extrasKeys.next());
        assertEquals("contact_content_str", extras.get("contact"));
    }
}
