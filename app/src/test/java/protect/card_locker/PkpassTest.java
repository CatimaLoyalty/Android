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
@Config(sdk = 23)
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
        // Expecting no English entries
        assertEquals(0, extras.getAllValues("en").keySet().size());
        // Expecting 7 untranslated entries
        assertEquals(7, extras.getAllValues("").keySet().size());
        // Untranslated and English + untranslated fallback should return the same
        assertEquals(extras.getAllValues(""), extras.getAllValues(new String[]{"en", ""}));

        // Check all 7 values
        Iterator<Map.Entry<String, String>> extrasKeys = extras.getAllValues("").entrySet().iterator();

        // 1
        Map.Entry<String, String> entry = extrasKeys.next();
        assertEquals("staffNumber", entry.getKey());
        assertEquals("Staff Number: 001", entry.getValue());

        // 2
        entry = extrasKeys.next();
        assertEquals("staffName", entry.getKey());
        assertEquals("Name: Peter Brooke", entry.getValue());

        // 3
        entry = extrasKeys.next();
        assertEquals("telephoneExt", entry.getKey());
        assertEquals("Extension: 9779", entry.getValue());

        // 4
        entry = extrasKeys.next();
        assertEquals("jobTitle", entry.getKey());
        assertEquals("Job Title: Chief Pass Creator", entry.getValue());

        // 5
        entry = extrasKeys.next();
        assertEquals("expiryDate", entry.getKey());
        assertEquals("Expiry Date: 2013-12-31T00:00-23:59", entry.getValue());

        // 6
        entry = extrasKeys.next();
        assertEquals("managersName", entry.getKey());
        assertEquals("Manager's Name: Paul Bailey", entry.getValue());

        // 7
        entry = extrasKeys.next();
        assertEquals("managersExt", entry.getKey());
        assertEquals("Manager's Extension: 9673", entry.getValue());
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

        // Expect 18 English values
        assertEquals(18, extras.getAllValues("en").size());

        // Expect 18 German values
        assertEquals(18, extras.getAllValues("de").size());

        // Expect 18 untranslated values
        assertEquals(18, extras.getAllValues("").size());

        // Expect no French values
        assertEquals(0, extras.getAllValues("fr").size());

        // Check all 18 English, German and untranslated values
        Iterator<Map.Entry<String, String>> englishKeys = extras.getAllValues("en").entrySet().iterator();
        Iterator<Map.Entry<String, String>> germanKeys = extras.getAllValues("de").entrySet().iterator();
        Iterator<Map.Entry<String, String>> untranslatedKeys = extras.getAllValues("").entrySet().iterator();

        // 1
        Map.Entry<String, String> englishEntry = englishKeys.next();
        Map.Entry<String, String> germanEntry = germanKeys.next();
        Map.Entry<String, String> untranslatedEntry = untranslatedKeys.next();
        assertEquals("gate", englishEntry.getKey());
        assertEquals("gate", germanEntry.getKey());
        assertEquals("gate", untranslatedEntry.getKey());
        assertEquals("Gate: B61", englishEntry.getValue());
        assertEquals("Gate: B61", germanEntry.getValue());
        assertEquals("gate_str: B61", untranslatedEntry.getValue());

        // 2
        englishEntry = englishKeys.next();
        germanEntry = germanKeys.next();
        untranslatedEntry = untranslatedKeys.next();
        assertEquals("seat", englishEntry.getKey());
        assertEquals("seat", germanEntry.getKey());
        assertEquals("seat", untranslatedEntry.getKey());
        assertEquals("Seat: 16E", englishEntry.getValue());
        assertEquals("Sitz: 16E", germanEntry.getValue());
        assertEquals("seat_str: 16E", untranslatedEntry.getValue());

        // 3
        englishEntry = englishKeys.next();
        germanEntry = germanKeys.next();
        untranslatedEntry = untranslatedKeys.next();
        assertEquals("origin", englishEntry.getKey());
        assertEquals("origin", germanEntry.getKey());
        assertEquals("origin", untranslatedEntry.getKey());
        assertEquals("Cologne-Bonn: CGN", englishEntry.getValue());
        assertEquals("Cologne-Bonn: CGN", germanEntry.getValue());
        assertEquals("Cologne-Bonn: CGN", untranslatedEntry.getValue());

        // 4
        englishEntry = englishKeys.next();
        germanEntry = germanKeys.next();
        untranslatedEntry = untranslatedKeys.next();
        assertEquals("destination", englishEntry.getKey());
        assertEquals("destination", germanEntry.getKey());
        assertEquals("destination", untranslatedEntry.getKey());
        assertEquals("Dubrovnik: DBV", englishEntry.getValue());
        assertEquals("Dubrovnik: DBV", germanEntry.getValue());
        assertEquals("Dubrovnik: DBV", untranslatedEntry.getValue());

        // 5
        englishEntry = englishKeys.next();
        germanEntry = germanKeys.next();
        untranslatedEntry = untranslatedKeys.next();
        assertEquals("name", englishEntry.getKey());
        assertEquals("name", germanEntry.getKey());
        assertEquals("name", untranslatedEntry.getKey());
        assertEquals("Name: John Doe", englishEntry.getValue());
        assertEquals("Name: John Doe", germanEntry.getValue());
        assertEquals("name_str: John Doe", untranslatedEntry.getValue());

        // 6
        englishEntry = englishKeys.next();
        germanEntry = germanKeys.next();
        untranslatedEntry = untranslatedKeys.next();
        assertEquals("status", englishEntry.getKey());
        assertEquals("status", germanEntry.getKey());
        assertEquals("status", untranslatedEntry.getKey());
        assertEquals("Status: -", englishEntry.getValue());
        assertEquals("Status: -", germanEntry.getValue());
        assertEquals("status_str: -", untranslatedEntry.getValue());

        // 7
        englishEntry = englishKeys.next();
        germanEntry = germanKeys.next();
        untranslatedEntry = untranslatedKeys.next();
        assertEquals("boardinggroup", englishEntry.getKey());
        assertEquals("boardinggroup", germanEntry.getKey());
        assertEquals("boardinggroup", untranslatedEntry.getKey());
        assertEquals("Group: GROUP 1", englishEntry.getValue());
        assertEquals("Gruppe: GROUP 1", germanEntry.getValue());
        assertEquals("boardinggroup_str: GROUP 1", untranslatedEntry.getValue());

        // 8
        englishEntry = englishKeys.next();
        germanEntry = germanKeys.next();
        untranslatedEntry = untranslatedKeys.next();
        assertEquals("tarif", englishEntry.getKey());
        assertEquals("tarif", germanEntry.getKey());
        assertEquals("tarif", untranslatedEntry.getKey());
        assertEquals("Fare: SMART", englishEntry.getValue());
        assertEquals("Tarif: SMART", germanEntry.getValue());
        assertEquals("fare_str: SMART", untranslatedEntry.getValue());

        // 9
        englishEntry = englishKeys.next();
        germanEntry = germanKeys.next();
        untranslatedEntry = untranslatedKeys.next();
        assertEquals("flightNumber", englishEntry.getKey());
        assertEquals("flightNumber", germanEntry.getKey());
        assertEquals("flightNumber", untranslatedEntry.getKey());
        assertEquals("Flight: EW 954", englishEntry.getValue());
        assertEquals("Flug: EW 954", germanEntry.getValue());
        assertEquals("flight_str: EW 954", untranslatedEntry.getValue());

        // 10
        englishEntry = englishKeys.next();
        germanEntry = germanKeys.next();
        untranslatedEntry = untranslatedKeys.next();
        assertEquals("departureDate", englishEntry.getKey());
        assertEquals("departureDate", germanEntry.getKey());
        assertEquals("departureDate", untranslatedEntry.getKey());
        assertEquals("Date: 08/09/2019", englishEntry.getValue());
        assertEquals("Datum: 08/09/2019", germanEntry.getValue());
        assertEquals("date_str: 08/09/2019", untranslatedEntry.getValue());

        // 11
        englishEntry = englishKeys.next();
        germanEntry = germanKeys.next();
        untranslatedEntry = untranslatedKeys.next();
        assertEquals("boarding", englishEntry.getKey());
        assertEquals("boarding", germanEntry.getKey());
        assertEquals("boarding", untranslatedEntry.getKey());
        assertEquals("Boarding: 05:00", englishEntry.getValue());
        assertEquals("Boarding: 05:00", germanEntry.getValue());
        assertEquals("boarding_str: 05:00", untranslatedEntry.getValue());

        // 12
        englishEntry = englishKeys.next();
        germanEntry = germanKeys.next();
        untranslatedEntry = untranslatedKeys.next();
        assertEquals("closure", englishEntry.getKey());
        assertEquals("closure", germanEntry.getKey());
        assertEquals("closure", untranslatedEntry.getKey());
        assertEquals("Gate closure: 05:15", englishEntry.getValue());
        assertEquals("Gate Schließt: 05:15", germanEntry.getValue());
        assertEquals("closure_str: 05:15", untranslatedEntry.getValue());

        // 13
        englishEntry = englishKeys.next();
        germanEntry = germanKeys.next();
        untranslatedEntry = untranslatedKeys.next();
        assertEquals("info", englishEntry.getKey());
        assertEquals("info", germanEntry.getKey());
        assertEquals("info", untranslatedEntry.getKey());
        assertEquals("Eurowings wishes you a pleasant flight .\r\n" +
                "\r\n" +
                "We kindly ask you to be present at your departure gate on time.", englishEntry.getValue());
        assertEquals("Eurowings wünscht Ihnen einen angenehmen Flug.\r\n" +
                "\r\n" +
                "Wir bitten Sie, sich zur angegeben Boarding Zeit am Gate einzufinden.", germanEntry.getValue());
        assertEquals("info_content_str", untranslatedEntry.getValue());

        // 14
        englishEntry = englishKeys.next();
        germanEntry = germanKeys.next();
        untranslatedEntry = untranslatedKeys.next();
        assertEquals("recordlocator", englishEntry.getKey());
        assertEquals("recordlocator", germanEntry.getKey());
        assertEquals("recordlocator", untranslatedEntry.getKey());
        assertEquals("Booking code: JBZPPP", englishEntry.getValue());
        assertEquals("Buchungscode: JBZPPP", germanEntry.getValue());
        assertEquals("recordlocator_str: JBZPPP", untranslatedEntry.getValue());

        // 15
        englishEntry = englishKeys.next();
        germanEntry = germanKeys.next();
        untranslatedEntry = untranslatedKeys.next();
        assertEquals("sequence", englishEntry.getKey());
        assertEquals("sequence", germanEntry.getKey());
        assertEquals("sequence", untranslatedEntry.getKey());
        assertEquals("Sequence: 73", englishEntry.getValue());
        assertEquals("Sequenz: 73", germanEntry.getValue());
        assertEquals("sequence_str: 73", untranslatedEntry.getValue());

        // 16
        englishEntry = englishKeys.next();
        germanEntry = germanKeys.next();
        untranslatedEntry = untranslatedKeys.next();
        assertEquals("notice", englishEntry.getKey());
        assertEquals("notice", germanEntry.getKey());
        assertEquals("notice", untranslatedEntry.getKey());
        assertEquals("Notice: Please note that although your flight may be delayed, you will still need to check in and go to your departure gate on time as scheduled.\r\n" +
                "\r\n" +
                "Carry on one item of hand luggage (8 kg, 55 x 40 x 23 cm) for free.", englishEntry.getValue());
        assertEquals("Hinweis: Bitte beachten Sie, dass obwohl Ihr Flug verspätet sein mag, Sie dennoch wie geplant pünktlich am Check-in und am Abfluggate erscheinen müssen.\r\n" +
                "\r\n" +
                "Kostenlose Mitnahme eines Handgepäckstücks (8 Kg, 55 x 40 x 23cm).", germanEntry.getValue());
        assertEquals("notice_str: notice_content_str", untranslatedEntry.getValue());

        // 17
        englishEntry = englishKeys.next();
        germanEntry = germanKeys.next();
        untranslatedEntry = untranslatedKeys.next();
        assertEquals("baggage", englishEntry.getKey());
        assertEquals("baggage", germanEntry.getKey());
        assertEquals("baggage", untranslatedEntry.getKey());
        assertEquals("Carrying liquids in hand luggage: In addition to other restrictions on hand luggage, there are still restrictions on liquids and gels brought by the passenger or purchased before the security control on all departures within the European Union, as well as to many other countries (including Switzerland, Russia, Iceland, Croatia, Israel, Egypt, Morocco, Tunisia and Norway):\r\n" +
                "\r\n" +
                "- All liquids (such as toiletries and cosmetics, gels, pastes, creams, lotions, mixtures of liquids and solids, perfumes, pressurised containers, cans, water bottles etc) as well as wax and gel-like substances may only be carried on board in amounts less than 100ml or 100g.\r\n" +
                "\r\n" +
                "- These liquids or substances must be packed in closed containers in a transparent, re-sealable plastic bag (max. contents 1 kg).\r\n" +
                "\r\n" +
                "- It is the passenger’s responsibility to purchase this bag before departure. They are available in many supermarkets, e.g. as freezer bags. It is currently not possible for passengers to obtain or purchase the required bags from Eurowings check-in.\r\n" +
                "\r\n" +
                "- Prescription medicines and baby food may still be carried in hand baggage. The passenger must prove that such medicines and/or baby food are needed during the flight.\r\n" +
                "\r\n" +
                "- Products and bags which do not meet the requirements or are only sealed with a rubber band or similar will unfortunately have to be surrendered by passengers\r\n" +
                "\r\n" +
                "In order to pass through the airport as quickly as possible, you are strongly advised to pack any liquids or gels which are not essential for your journey on board the aircraft in your checked baggage if possible.\r\n" +
                "\r\n" +
                "As a matter of course, liquids from the Travel Value / Duty Free shops which have been purchased after you have passed through security are still allowed on board.\r\n" +
                "\r\n" +
                "Eurowings shall not be liable for any items which passengers are prohibited from carrying in their hand baggage for security reasons and are required to surrender at the security checkpoint.", englishEntry.getValue());
        assertEquals("Mitnahme von Flüssigkeiten im Handgepäck: Neben den sonstigen Beschränkungen für das Handgepäck ist für alle Abflüge innerhalb der Europäischen Union sowie vielen weiteren Ländern (u.a. Schweiz, Russland, Island, Kroatien, Israel, Ägypten, Marokko, Tunesien, Norwegen)  die Mitnahme von vor der Fluggastkontrolle erworbenen bzw. mitgebrachten Flüssigkeiten und Gels nur noch eingeschränkt erlaubt:\r\n" +
                "\r\n" +
                "- Sämtliche Flüssigkeiten (wie Kosmetik- und Toilettenartikel, Gels, Pasten, Cremes, Lotionen, Gemische aus flüssigen und festen Stoffen, Parfums, Behälter unter Druck, Dosen, Wasserflaschen etc.) sowie wachs- oder gelartige Stoffe dürfen nur noch in Behältnissen bis zu 100 ml bzw. 100 g mit an Bord genommen werden.\r\n" +
                "\r\n" +
                "- Diese Flüssigkeiten bzw. Stoffe müssen in einem transparenten, wiederverschließbaren Plastikbeutel (max. 1 kg Inhalt) vollständig geschlossen, verpackt sein.\r\n" +
                "\r\n" +
                "- Diese Beutel müssen Fluggäste selbst vor dem Abflug erwerben. Sie sind in vielen Supermärkten z. B. als Gefrierbeutel erhältlich. Es besteht zurzeit keine Möglichkeit, entsprechende Plastikbeutel am Eurowings Check-In zu erwerben bzw. auszugeben.\r\n" +
                "\r\n" +
                "- Verschreibungspflichtige Medikamente sowie Babynahrung dürfen weiterhin im Handgepäck transportiert werden. Der Fluggast muss nachweisen, dass die Medikamente und Babynahrung während des Fluges benötigt werden.\r\n" +
                "\r\n" +
                "- Produkte und Beutel, die nicht den Maßgaben entsprechen oder die nur mit Gummiband oder ähnlichem verschlossen sind, müssen leider abgegeben werden.\r\n" +
                "\r\n" +
                "Flüssigkeiten und Gels, die Sie nicht zwingend während Ihres Aufenthalts an Bord benötigen, sollten zur raschen Fluggastabfertigung nach Möglichkeit im aufzugebenden Gepäck untergebracht werden.\r\n" +
                "\r\n" +
                "Selbstverständlich ist die Mitnahme von allen Flüssigkeiten/Gels/Getränken aus Travel-Value oder Duty Free-Shops, die nach der Fluggastkontrolle erworben werden, weiterhin erlaubt.\r\n" +
                "\r\n" +
                "Eurowings übernimmt keine Haftung für Gegenstände, die der Fluggast nicht im Handgepäck mitführen darf und deshalb aus Sicherheitsgründen an der Fluggastkontrolle abgeben muss.", germanEntry.getValue());
        assertEquals("baggage_str: baggage_content_str", untranslatedEntry.getValue());

        // 18
        englishEntry = englishKeys.next();
        germanEntry = germanKeys.next();
        untranslatedEntry = untranslatedKeys.next();
        assertEquals("contact", englishEntry.getKey());
        assertEquals("contact", germanEntry.getKey());
        assertEquals("contact", untranslatedEntry.getKey());
        assertEquals("Contact: https://mobile.eurowings.com/booking/StaticContactInfo.aspx?culture=en-GB&back=home", englishEntry.getValue());
        assertEquals("Kontakt: Sie erreichen das deutsche Call Center unter der Telefonnummer\r\n" +
                "\r\n" +
                "0180 6 320 320 ( 0:00 Uhr - 24:00 Uhr )\r\n" +
                "\r\n" +
                "(0,20 € pro Anruf aus dem Festnetz der Deutschen Telekom - Mobilfunk maximal 0,60 € pro Anruf).", germanEntry.getValue());
        assertEquals("contact_str: contact_content_str", untranslatedEntry.getValue());
    }
}
