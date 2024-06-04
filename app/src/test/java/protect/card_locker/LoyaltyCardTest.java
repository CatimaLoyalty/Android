package protect.card_locker;

import android.os.Parcel;
import android.os.Parcelable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Date;

import com.google.zxing.BarcodeFormat;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class LoyaltyCardTest {

    @Test
    public void testParcelable() {
        Date validFrom = new Date();
        Date expiry = new Date();
        BigDecimal balance = new BigDecimal("100.00");
        Currency currency = Currency.getInstance("USD");
        LoyaltyCard card = new LoyaltyCard(1, "Store A", "Note A", validFrom, expiry, balance, currency, "12345", "67890", CatimaBarcode.fromBarcode(BarcodeFormat.QR_CODE), 0xFF0000, 1, System.currentTimeMillis(), 10, 0);

        Parcel parcel = Parcel.obtain();
        card.writeToParcel(parcel, card.describeContents());

        parcel.setDataPosition(0);

        LoyaltyCard createdFromParcel = LoyaltyCard.CREATOR.createFromParcel(parcel);

        assertEquals(card.id, createdFromParcel.id);
        assertEquals(card.store, createdFromParcel.store);
        assertEquals(card.note, createdFromParcel.note);
        assertEquals(card.validFrom, createdFromParcel.validFrom);
        assertEquals(card.expiry, createdFromParcel.expiry);
        assertEquals(card.balance, createdFromParcel.balance);
        assertEquals(card.balanceType, createdFromParcel.balanceType);
        assertEquals(card.cardId, createdFromParcel.cardId);
        assertEquals(card.barcodeId, createdFromParcel.barcodeId);
        assertEquals(card.barcodeType.name(), createdFromParcel.barcodeType.name());
        assertEquals(card.headerColor, createdFromParcel.headerColor);
        assertEquals(card.starStatus, createdFromParcel.starStatus);
        assertEquals(card.lastUsed, createdFromParcel.lastUsed);
        assertEquals(card.zoomLevel, createdFromParcel.zoomLevel);
        assertEquals(card.archiveStatus, createdFromParcel.archiveStatus);

        parcel.recycle();
    }

    @Test
    public void testIsDuplicate_sameObject() {
        Date now = new Date();
        BigDecimal balance = new BigDecimal("50.00");
        Currency currency = Currency.getInstance("EUR");
        LoyaltyCard card1 = new LoyaltyCard(1, "Store B", "Note B", now, now, balance, currency, "22222", "33333", CatimaBarcode.fromBarcode(BarcodeFormat.PDF_417), 0x00FF00, 1, System.currentTimeMillis(), 5, 1);

        assertTrue(LoyaltyCard.isDuplicate(card1, card1));
    }

    @Test
    public void testIsDuplicate_differentObjects() {
        Date now = new Date();
        BigDecimal balance1 = new BigDecimal("50.00");
        BigDecimal balance2 = new BigDecimal("75.00");
        Currency currency = Currency.getInstance("EUR");
        LoyaltyCard card1 = new LoyaltyCard(2, "Store C", "Note C", now, now, balance1, currency, "44444", "55555", CatimaBarcode.fromBarcode(BarcodeFormat.DATA_MATRIX), 0x0000FF, 0, System.currentTimeMillis(), 15, 1);
        LoyaltyCard card2 = new LoyaltyCard(2, "Store C", "Note C", now, now, balance2, currency, "44444", "55555", CatimaBarcode.fromBarcode(BarcodeFormat.DATA_MATRIX), 0x0000FF, 0, System.currentTimeMillis(), 15, 1);

        assertFalse(LoyaltyCard.isDuplicate(card1, card2));
    }

    @Test
    public void testToString() {
        Date now = new Date();
        BigDecimal balance = new BigDecimal("100.00");
        Currency currency = Currency.getInstance("USD");
        LoyaltyCard card = new LoyaltyCard(3, "Store D", "Note D", now, now, balance, currency, "66666", "77777", CatimaBarcode.fromBarcode(BarcodeFormat.AZTEC), null, 2, System.currentTimeMillis(), 20, 2);
        
        String expected = String.format(
                "LoyaltyCard{%n  id=%s,%n  store=%s,%n  note=%s,%n  validFrom=%s,%n  expiry=%s,%n"
                        + "  balance=%s,%n  balanceType=%s,%n  cardId=%s,%n  barcodeId=%s,%n  barcodeType=%s,%n"
                        + "  headerColor=%s,%n  starStatus=%s,%n  lastUsed=%s,%n  zoomLevel=%s,%n  archiveStatus=%s%n}",
                card.id, card.store, card.note, card.validFrom, card.expiry,
                card.balance, card.balanceType, card.cardId, card.barcodeId,
                card.barcodeType != null ? card.barcodeType.format() : null,
                card.headerColor, card.starStatus, card.lastUsed, card.zoomLevel, card.archiveStatus);

        assertEquals(expected, card.toString());
    }
}
