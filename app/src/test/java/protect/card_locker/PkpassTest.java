package protect.card_locker;

import android.app.Activity;
import android.graphics.Color;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

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
    public void parseGenericExample() throws JSONException
    {
        // Generic example from https://github.com/keefmoon/Passbook-Example-Code/blob/master/Pass-Example-Generic/Pass-Example-Generic.pkpass
        JSONObject json = new JSONObject("{\n" +
                "  \"passTypeIdentifier\" : \"pass.keefmoon.example-generic\",\n" +
                "  \"formatVersion\" : 1,\n" +
                "  \"teamIdentifier\" : \"YAXJZJ267E\",\n" +
                "  \"organizationName\" : \"Passbook Example Company\",\n" +
                "  \"serialNumber\" : \"0000001\",\n" +
                "  \"description\" : \"Staff Pass for Employee Number 001\",\n" +
                "  \"associatedStoreIdentifiers\" : [\n" +
                "  \t 284971959\n" +
                "  ],\n" +
                "  \"locations\" : [\n" +
                "  \t{\"latitude\" : 51.50506, \"longitude\" : -0.01960, \"relevantText\" : \"Company Offices\" }\n" +
                "  ],\n" +
                "  \"foregroundColor\" : \"rgb(255, 255, 255)\",\n" +
                "  \"backgroundColor\" : \"rgb(90, 90, 90)\",\n" +
                "  \"labelColor\" : \"rgb(255, 255, 255)\",\n" +
                "  \"logoText\" : \"Company Staff ID\",\n" +
                "  \"barcode\" : {\n" +
                "  \t\t\"format\" : \"PKBarcodeFormatQR\",\n" +
                "  \t\t\"message\" : \"0000001\",\n" +
                "  \t\t\"messageEncoding\" : \"iso-8859-1\",\n" +
                "  \t\t\"altText\" : \"Staff ID 0000001\"\n" +
                "   },\n" +
                "  \"generic\" : {\n" +
                "  \t\"headerFields\" : [\n" +
                "  \t\t{\n" +
                "  \t\t\t\"key\" : \"staffNumber\",\n" +
                "        \t\"label\" : \"Staff Number\",\n" +
                "        \t\"value\" : \"001\"\n" +
                "  \t\t}\n" +
                "  \t],\n" +
                "    \"primaryFields\" : [\n" +
                "      {\n" +
                "        \"key\" : \"staffName\",\n" +
                "        \"label\" : \"Name\",\n" +
                "        \"value\" : \"Peter Brooke\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"secondaryFields\" : [\n" +
                "      {\n" +
                "        \"key\" : \"telephoneExt\",\n" +
                "        \"label\" : \"Extension\",\n" +
                "        \"value\" : \"9779\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"key\" : \"jobTitle\",\n" +
                "        \"label\" : \"Job Title\",\n" +
                "        \"value\" : \"Chief Pass Creator\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"auxiliaryFields\" : [\n" +
                "      {\n" +
                "        \"key\" : \"expiryDate\",\n" +
                "        \"dateStyle\" : \"PKDateStyleShort\",\n" +
                "        \"label\" : \"Expiry Date\",\n" +
                "        \"value\" : \"2013-12-31T00:00-23:59\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"backFields\" : [\n" +
                "      {\n" +
                "        \"key\" : \"managersName\",\n" +
                "        \"label\" : \"Manager's Name\",\n" +
                "        \"value\" : \"Paul Bailey\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"key\" : \"managersExt\",\n" +
                "        \"label\" : \"Manager's Extension\",\n" +
                "        \"value\" : \"9673\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}");

        // Parse Pkpass JSON
        LoyaltyCard card = pkpassImporter.fromPassJSON(json);

        // Compare everything
        assertEquals(card.barcodeType, "QR_CODE");
        assertEquals(card.cardId, "0000001");
        assertEquals(card.note, "Staff Pass for Employee Number 001");
        assertEquals(card.store, "Passbook Example Company");
        assertEquals(card.headerColor, String.valueOf(Color.rgb(90, 90, 90)));
        assertEquals(card.headerTextColor, String.valueOf(Color.rgb(255, 255, 255)));
    }
}
