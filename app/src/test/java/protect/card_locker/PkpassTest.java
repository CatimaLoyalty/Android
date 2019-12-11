package protect.card_locker;

import android.app.Activity;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;

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
        assertEquals(BarcodeFormat.QR_CODE.name(), card.barcodeType);
        assertEquals("0000001", card.cardId);
        assertEquals("Staff Pass for Employee Number 001", card.note);
        assertEquals("Passbook Example Company", card.store);
        assertEquals(String.valueOf(Color.rgb(90, 90, 90)), card.headerColor.toString());
        assertEquals(String.valueOf(Color.rgb(255, 255, 255)), card.headerTextColor.toString());
    }

    @Test
    public void parseEurowingsTicket() throws JSONException
    {
        // https://github.com/brarcher/loyalty-card-locker/issues/309#issuecomment-563465333
        JSONObject json = new JSONObject("{\"description\":\"Eurowings Boarding Pass\",\"formatVersion\":1,\"organizationName\":\"EUROWINGS\",\"passTypeIdentifier\":\"pass.wings.boardingpass\",\"serialNumber\":\"quUBzi53bVIzpO2R15jEzRdG54Pb4qPbggEXZT7XKWx6AN%2bg8nfShLvh9teneKWV\",\"teamIdentifier\":\"M2DAP2XTGE\",\"authenticationToken\":\"d5828a30d7644eeffa9e900ac91ac6ec\",\"webServiceURL\":\"https://mobile.eurowings.com/booking/scripts/Passbook/PassbookPassGenerator.aspx?\",\"relevantDate\":\"2019-09-08T05:00+02:00\",\"backgroundColor\":\"#FFFFFF\",\"foregroundColor\":\"#333333\",\"labelColor\":\"#AA0061\",\"boardingPass\":{\"transitType\":\"PKTransitTypeAir\",\"headerFields\":[{\"key\":\"gate\",\"label\":\"gate_str\",\"value\":\"B61\"},{\"key\":\"seat\",\"label\":\"seat_str\",\"value\":\"16E\"}],\"primaryFields\":[{\"key\":\"origin\",\"label\":\"Cologne-Bonn\",\"value\":\"CGN\"},{\"key\":\"destination\",\"label\":\"Dubrovnik\",\"value\":\"DBV\"}],\"secondaryFields\":[{\"key\":\"name\",\"label\":\"name_str\",\"value\":\"John Doe\",\"textAlignment\":\"PKTextAlignmentLeft\"},{\"key\":\"status\",\"label\":\"status_str\",\"value\":\"-\",\"textAlignment\":\"PKTextAlignmentLeft\"},{\"key\":\"boardinggroup\",\"label\":\"boardinggroup_str\",\"value\":\"GROUP 1\",\"textAlignment\":\"PKTextAlignmentLeft\"},{\"key\":\"tarif\",\"label\":\"fare_str\",\"value\":\"SMART\",\"textAlignment\":\"PKTextAlignmentLeft\"}],\"auxiliaryFields\":[{\"key\":\"flightNumber\",\"label\":\"flight_str\",\"value\":\"EW 954\",\"textAlignment\":\"PKTextAlignmentLeft\"},{\"key\":\"departureDate\",\"label\":\"date_str\",\"value\":\"08/09/2019\",\"textAlignment\":\"PKTextAlignmentLeft\"},{\"key\":\"boarding\",\"label\":\"boarding_str\",\"value\":\"05:00\",\"textAlignment\":\"PKTextAlignmentLeft\"},{\"key\":\"closure\",\"label\":\"closure_str\",\"value\":\"05:15\",\"textAlignment\":\"PKTextAlignmentLeft\"}],\"backFields\":[{\"key\":\"info\",\"label\":\"\",\"value\":\"info_content_str\",\"textAlignment\":\"PKTextAlignmentLeft\"},{\"key\":\"recordlocator\",\"label\":\"recordlocator_str\",\"value\":\"JBZPPP\",\"textAlignment\":\"PKTextAlignmentLeft\"},{\"key\":\"sequence\",\"label\":\"sequence_str\",\"value\":\"73\",\"textAlignment\":\"PKTextAlignmentLeft\"},{\"key\":\"notice\",\"label\":\"notice_str\",\"value\":\"notice_content_str\",\"textAlignment\":\"PKTextAlignmentLeft\"},{\"key\":\"baggage\",\"label\":\"baggage_str\",\"value\":\"baggage_content_str\",\"textAlignment\":\"PKTextAlignmentLeft\"},{\"key\":\"contact\",\"label\":\"contact_str\",\"value\":\"contact_content_str\",\"textAlignment\":\"PKTextAlignmentLeft\"}]},\"barcode\":{\"format\":\"PKBarcodeFormatAztec\",\"message\":\"M1DOE/JOHN         JBZPPP CGNDBVEW 0954 251A016E0073 148>5181W 9250BEW 00000000000002A0000000000000 0                          N\",\"messageEncoding\":\"iso-8859-1\"}}");

        // Parse Pkpass JSON
        LoyaltyCard card = pkpassImporter.fromPassJSON(json);

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
