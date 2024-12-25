package protect.card_locker

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.zxing.BarcodeFormat
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowContentResolver
import org.robolectric.shadows.ShadowLog
import java.math.BigDecimal
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class PkpassTest {
    @Before
    fun setUp() {
        ShadowLog.stream = System.out
    }

    @Test
    fun testEurowingsPass() {
        // Prepare
        val context: Context = ApplicationProvider.getApplicationContext()
        val pkpass = "pkpass/Eurowings/Eurowings.pkpass"
        val image = "pkpass/Eurowings/logo@2x.png"

        val pkpassUri = Uri.parse(pkpass)
        val imageUri = Uri.parse(image)
        ShadowContentResolver().registerInputStream(pkpassUri, javaClass.getResourceAsStream(pkpass))
        ShadowContentResolver().registerInputStream(imageUri, javaClass.getResourceAsStream(image))

        val parser = PkpassParser(context, pkpassUri)
        val imageBitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(imageUri))

        // Confirm this does not have languages
        Assert.assertEquals(listOf("de", "en"), parser.listLocales())

        // Confirm correct parsing (en)
        var parsedCard = parser.toLoyaltyCard("de")

        Assert.assertEquals(-1, parsedCard.id)
        Assert.assertEquals("EUROWINGS", parsedCard.store)
        Assert.assertEquals("Eurowings Boarding Pass\n" +
                "\n" +
                "Gate: B61\n" +
                "Sitz: 12D\n" +
                "\n" +
                "Cologne-Bonn: CGN\n" +
                "Dubrovnik: DBV\n" +
                "\n" +
                "Name: John Doe\n" +
                "Status: -\n" +
                "Gruppe: GROUP 1\n" +
                "Tarif: SMART\n" +
                "\n" +
                "Flug: EW 954\n" +
                "Datum: 08/09/2019\n" +
                "Boarding: 05:00\n" +
                "Gate Schließt: 05:15\n" +
                "\n" +
                "Eurowings wünscht Ihnen einen angenehmen Flug.\n" +
                "\n" +
                "Wir bitten Sie, sich zur angegeben Boarding Zeit am Gate einzufinden.\n" +
                "Buchungscode: JBZPPP\n" +
                "Sequenz: 73\n" +
                "Hinweis: Bitte beachten Sie, dass obwohl Ihr Flug verspätet sein mag, Sie dennoch wie geplant pünktlich am Check-in und am Abfluggate erscheinen müssen.\n" +
                "\n" +
                "Kostenlose Mitnahme eines Handgepäckstücks (8 Kg, 55 x 40 x 23cm).\n" +
                "Mitnahme von Flüssigkeiten im Handgepäck: Neben den sonstigen Beschränkungen für das Handgepäck ist für alle Abflüge innerhalb der Europäischen Union sowie vielen weiteren Ländern (u.a. Schweiz, Russland, Island, Kroatien, Israel, Ägypten, Marokko, Tunesien, Norwegen)  die Mitnahme von vor der Fluggastkontrolle erworbenen bzw. mitgebrachten Flüssigkeiten und Gels nur noch eingeschränkt erlaubt:\n" +
                "\n" +
                "- Sämtliche Flüssigkeiten (wie Kosmetik- und Toilettenartikel, Gels, Pasten, Cremes, Lotionen, Gemische aus flüssigen und festen Stoffen, Parfums, Behälter unter Druck, Dosen, Wasserflaschen etc.) sowie wachs- oder gelartige Stoffe dürfen nur noch in Behältnissen bis zu 100 ml bzw. 100 g mit an Bord genommen werden.\n" +
                "\n" +
                "- Diese Flüssigkeiten bzw. Stoffe müssen in einem transparenten, wiederverschließbaren Plastikbeutel (max. 1 kg Inhalt) vollständig geschlossen, verpackt sein.\n" +
                "\n" +
                "- Diese Beutel müssen Fluggäste selbst vor dem Abflug erwerben. Sie sind in vielen Supermärkten z. B. als Gefrierbeutel erhältlich. Es besteht zurzeit keine Möglichkeit, entsprechende Plastikbeutel am Eurowings Check-In zu erwerben bzw. auszugeben.\n" +
                "\n" +
                "- Verschreibungspflichtige Medikamente sowie Babynahrung dürfen weiterhin im Handgepäck transportiert werden. Der Fluggast muss nachweisen, dass die Medikamente und Babynahrung während des Fluges benötigt werden.\n" +
                "\n" +
                "- Produkte und Beutel, die nicht den Maßgaben entsprechen oder die nur mit Gummiband oder ähnlichem verschlossen sind, müssen leider abgegeben werden.\n" +
                "\n" +
                "Flüssigkeiten und Gels, die Sie nicht zwingend während Ihres Aufenthalts an Bord benötigen, sollten zur raschen Fluggastabfertigung nach Möglichkeit im aufzugebenden Gepäck untergebracht werden.\n" +
                "\n" +
                "Selbstverständlich ist die Mitnahme von allen Flüssigkeiten/Gels/Getränken aus Travel-Value oder Duty Free-Shops, die nach der Fluggastkontrolle erworben werden, weiterhin erlaubt.\n" +
                "\n" +
                "Eurowings übernimmt keine Haftung für Gegenstände, die der Fluggast nicht im Handgepäck mitführen darf und deshalb aus Sicherheitsgründen an der Fluggastkontrolle abgeben muss.\n" +
                "Kontakt: Sie erreichen das deutsche Call Center unter der Telefonnummer\n" +
                "\n" +
                "0180 6 320 320 ( 0:00 Uhr - 24:00 Uhr )\n" +
                "\n" +
                "(0,20 € pro Anruf aus dem Festnetz der Deutschen Telekom - Mobilfunk maximal 0,60 € pro Anruf).", parsedCard.note)
        Assert.assertEquals(Date(1567911600000), parsedCard.validFrom)
        Assert.assertEquals(null, parsedCard.expiry)
        Assert.assertEquals(BigDecimal(0), parsedCard.balance)
        Assert.assertEquals(null, parsedCard.balanceType)
        Assert.assertEquals("M1DOE/JOHN         JBZPPP CGNDBVEW 0954 251A012D0073 148>5181W 9250BEW 00000000000002A0000000000000 0                          N", parsedCard.cardId)
        Assert.assertEquals(null, parsedCard.barcodeId)
        Assert.assertEquals(BarcodeFormat.AZTEC, parsedCard.barcodeType!!.format())
        Assert.assertEquals(Color.parseColor("#FFFFFF"), parsedCard.headerColor)
        Assert.assertEquals(0, parsedCard.starStatus)
        Assert.assertEquals(0, parsedCard.archiveStatus)
        Assert.assertEquals(0, parsedCard.lastUsed)
        Assert.assertEquals(DBHelper.DEFAULT_ZOOM_LEVEL, parsedCard.zoomLevel)
        Assert.assertEquals(DBHelper.DEFAULT_ZOOM_LEVEL_WIDTH, parsedCard.zoomLevelWidth)

        // Confirm correct image is used
        Assert.assertTrue(imageBitmap.sameAs(parser.image))

        // Confirm correct parsing (en)
        parsedCard = parser.toLoyaltyCard("en")

        Assert.assertEquals(-1, parsedCard.id)
        Assert.assertEquals("EUROWINGS", parsedCard.store)
        Assert.assertEquals("Eurowings Boarding Pass\n" +
                "\n" +
                "Gate: B61\n" +
                "Seat: 12D\n" +
                "\n" +
                "Cologne-Bonn: CGN\n" +
                "Dubrovnik: DBV\n" +
                "\n" +
                "Name: John Doe\n" +
                "Status: -\n" +
                "Group: GROUP 1\n" +
                "Fare: SMART\n" +
                "\n" +
                "Flight: EW 954\n" +
                "Date: 08/09/2019\n" +
                "Boarding: 05:00\n" +
                "Gate closure: 05:15\n" +
                "\n" +
                "Eurowings wishes you a pleasant flight .\n" +
                "\n" +
                "We kindly ask you to be present at your departure gate on time.\n" +
                "Booking code: JBZPPP\n" +
                "Sequence: 73\n" +
                "Notice: Please note that although your flight may be delayed, you will still need to check in and go to your departure gate on time as scheduled.\n" +
                "\n" +
                "Carry on one item of hand luggage (8 kg, 55 x 40 x 23 cm) for free.\n" +
                "Carrying liquids in hand luggage: In addition to other restrictions on hand luggage, there are still restrictions on liquids and gels brought by the passenger or purchased before the security control on all departures within the European Union, as well as to many other countries (including Switzerland, Russia, Iceland, Croatia, Israel, Egypt, Morocco, Tunisia and Norway):\n" +
                "\n" +
                "- All liquids (such as toiletries and cosmetics, gels, pastes, creams, lotions, mixtures of liquids and solids, perfumes, pressurised containers, cans, water bottles etc) as well as wax and gel-like substances may only be carried on board in amounts less than 100ml or 100g.\n" +
                "\n" +
                "- These liquids or substances must be packed in closed containers in a transparent, re-sealable plastic bag (max. contents 1 kg).\n" +
                "\n" +
                "- It is the passenger’s responsibility to purchase this bag before departure. They are available in many supermarkets, e.g. as freezer bags. It is currently not possible for passengers to obtain or purchase the required bags from Eurowings check-in.\n" +
                "\n" +
                "- Prescription medicines and baby food may still be carried in hand baggage. The passenger must prove that such medicines and/or baby food are needed during the flight.\n" +
                "\n" +
                "- Products and bags which do not meet the requirements or are only sealed with a rubber band or similar will unfortunately have to be surrendered by passengers\n" +
                "\n" +
                "In order to pass through the airport as quickly as possible, you are strongly advised to pack any liquids or gels which are not essential for your journey on board the aircraft in your checked baggage if possible.\n" +
                "\n" +
                "As a matter of course, liquids from the Travel Value / Duty Free shops which have been purchased after you have passed through security are still allowed on board.\n" +
                "\n" +
                "Eurowings shall not be liable for any items which passengers are prohibited from carrying in their hand baggage for security reasons and are required to surrender at the security checkpoint.\n" +
                "Contact: https://mobile.eurowings.com/booking/StaticContactInfo.aspx?culture=en-GB&back=home", parsedCard.note)
        Assert.assertEquals(Date(1567911600000), parsedCard.validFrom)
        Assert.assertEquals(null, parsedCard.expiry)
        Assert.assertEquals(BigDecimal(0), parsedCard.balance)
        Assert.assertEquals(null, parsedCard.balanceType)
        Assert.assertEquals("M1DOE/JOHN         JBZPPP CGNDBVEW 0954 251A012D0073 148>5181W 9250BEW 00000000000002A0000000000000 0                          N", parsedCard.cardId)
        Assert.assertEquals(null, parsedCard.barcodeId)
        Assert.assertEquals(BarcodeFormat.AZTEC, parsedCard.barcodeType!!.format())
        Assert.assertEquals(Color.parseColor("#FFFFFF"), parsedCard.headerColor)
        Assert.assertEquals(0, parsedCard.starStatus)
        Assert.assertEquals(0, parsedCard.archiveStatus)
        Assert.assertEquals(0, parsedCard.lastUsed)
        Assert.assertEquals(DBHelper.DEFAULT_ZOOM_LEVEL, parsedCard.zoomLevel)
        Assert.assertEquals(DBHelper.DEFAULT_ZOOM_LEVEL_WIDTH, parsedCard.zoomLevelWidth)

        // Confirm correct image is used
        Assert.assertTrue(imageBitmap.sameAs(parser.image))
    }

    @Test
    fun testDCBPkPass() {
        // Prepare
        val context: Context = ApplicationProvider.getApplicationContext()
        val pkpass = "pkpass/DCBLN24/DCBLN24-QLUKT-1-passbook.pkpass"
        val image = "pkpass/DCBLN24/logo.png"

        val pkpassUri = Uri.parse(pkpass)
        val imageUri = Uri.parse(image)
        ShadowContentResolver().registerInputStream(pkpassUri, javaClass.getResourceAsStream(pkpass))
        ShadowContentResolver().registerInputStream(imageUri, javaClass.getResourceAsStream(image))

        val parser = PkpassParser(context, pkpassUri)
        val imageBitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(imageUri))

        // Confirm this does not have languages
        Assert.assertEquals(listOf<String>(), parser.listLocales())

        // Confirm correct parsing
        val parsedCard = parser.toLoyaltyCard(null)

        Assert.assertEquals(-1, parsedCard.id)
        Assert.assertEquals("droidcon Berlin 2024", parsedCard.store)
        Assert.assertEquals("Ticket for droidcon Berlin 2024 (Speaker)\n" +
                "\n" +
                "Admission time: 2024-07-03 08:00\n" +
                "\n" +
                "Event: droidcon Berlin 2024\n" +
                "\n" +
                "Product: Speaker\n" +
                "\n" +
                "Attendee name: Sylvia van Os\n" +
                "From: 2024-07-03 08:00\n" +
                "To: 2024-07-05 18:30\n" +
                "\n" +
                "Admission time: 2024-07-03 08:00\n" +
                "Attendee name: Sylvia van Os\n" +
                "Ordered by: REDACTED@example.com\n" +
                "Organizer: droidcon\n" +
                "Organizer contact: global@droidcon.de\n" +
                "Order code: REDACTED\n" +
                "Purchase date: 2024-06-06 07:26\n" +
                "Website: https://pretix.eu/droidcon/dcbln24/", parsedCard.note)
        Assert.assertEquals(null, parsedCard.validFrom)
        Assert.assertEquals(null, parsedCard.expiry)
        Assert.assertEquals(BigDecimal(0), parsedCard.balance)
        Assert.assertEquals(null, parsedCard.balanceType)
        Assert.assertEquals("ca4phaix1ahkahD2eiVi5iepahxa6rei", parsedCard.cardId)
        Assert.assertEquals(null, parsedCard.barcodeId)
        Assert.assertEquals(BarcodeFormat.QR_CODE, parsedCard.barcodeType!!.format())
        Assert.assertEquals(Color.parseColor("#0014e6"), parsedCard.headerColor)
        Assert.assertEquals(0, parsedCard.starStatus)
        Assert.assertEquals(0, parsedCard.archiveStatus)
        Assert.assertEquals(0, parsedCard.lastUsed)
        Assert.assertEquals(DBHelper.DEFAULT_ZOOM_LEVEL, parsedCard.zoomLevel)
        Assert.assertEquals(DBHelper.DEFAULT_ZOOM_LEVEL_WIDTH, parsedCard.zoomLevelWidth)

        // Confirm correct image is used
        Assert.assertTrue(imageBitmap.sameAs(parser.image))
    }
}
