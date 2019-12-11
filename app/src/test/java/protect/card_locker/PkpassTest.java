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
    }
}
