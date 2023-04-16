package protect.card_locker;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.Currency;
import java.util.Date;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.preference.PreferenceManager;
import protect.card_locker.preferences.Settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class LoyaltyCardCursorAdapterTest {
    private Activity activity;
    private SQLiteDatabase mDatabase;
    private SharedPreferences settings;

    @Before
    public void setUp() {
        ShadowLog.stream = System.out;

        activity = Robolectric.setupActivity(MainActivity.class);
        mDatabase = TestHelpers.getEmptyDb(activity).getWritableDatabase();
        settings = PreferenceManager.getDefaultSharedPreferences(activity);
    }

    private void setFontScale(int fontSizeScale) {
        settings.edit()
                .putInt(activity.getResources().getString(R.string.settings_key_max_font_size_scale), fontSizeScale)
                .apply();
    }

    private View createView(Cursor cursor) {
        LoyaltyCardCursorAdapter adapter = new LoyaltyCardCursorAdapter(activity.getApplicationContext(), cursor, (MainActivity) activity);

        LoyaltyCardCursorAdapter.LoyaltyCardListItemViewHolder viewHolder = adapter.createViewHolder(activity.findViewById(R.id.list), 0);
        adapter.bindViewHolder(viewHolder, cursor.getPosition());

        return viewHolder.itemView;
    }

    private void checkView(final View view, final String store, final String note, final String validFrom, final String expiry, final String balance, boolean checkFontSizes) {
        final TextView storeField = view.findViewById(R.id.store);
        final TextView noteField = view.findViewById(R.id.note);
        final TextView validFromField = view.findViewById(R.id.validFrom);
        final TextView expiryField = view.findViewById(R.id.expiry);
        final TextView balanceField = view.findViewById(R.id.balance);

        if (checkFontSizes) {
            Settings preferences = new Settings(activity.getApplicationContext());
            int mediumFontSize = preferences.getFontSizeMax(preferences.getMediumFont());
            int smallFontSize = preferences.getFontSizeMax(preferences.getSmallFont());

            assertEquals(mediumFontSize, (int) storeField.getTextSize());
            assertEquals(smallFontSize, (int) noteField.getTextSize());
            assertEquals(smallFontSize, (int) validFromField.getTextSize());
            assertEquals(smallFontSize, (int) expiryField.getTextSize());
        }

        assertEquals(store, storeField.getText().toString());
        if (!note.isEmpty()) {
            assertEquals(View.VISIBLE, noteField.getVisibility());
            assertEquals(note, noteField.getText().toString());
        } else {
            assertEquals(View.GONE, noteField.getVisibility());
        }

        if (!validFrom.isEmpty()) {
            assertEquals(View.VISIBLE, validFromField.getVisibility());
            assertEquals(validFrom, validFromField.getText().toString());
        } else {
            assertEquals(View.GONE, validFromField.getVisibility());
        }

        if (!expiry.isEmpty()) {
            assertEquals(View.VISIBLE, expiryField.getVisibility());
            assertEquals(expiry, expiryField.getText().toString());
        } else {
            assertEquals(View.GONE, expiryField.getVisibility());
        }

        if (!balance.isEmpty()) {
            assertEquals(View.VISIBLE, balanceField.getVisibility());
            assertEquals(balance, balanceField.getText().toString());
        } else {
            assertEquals(View.GONE, balanceField.getVisibility());
        }
    }


    @Test
    public void TestCursorAdapterEmptyNote() {
        DBHelper.insertLoyaltyCard(mDatabase, "store", "", null, null, new BigDecimal("0"), null, "cardId", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), Color.BLACK, 0, null,0);
        LoyaltyCard card = DBHelper.getLoyaltyCard(mDatabase, 1);

        Cursor cursor = DBHelper.getLoyaltyCardCursor(mDatabase);
        cursor.moveToFirst();

        View view = createView(cursor);

        checkView(view, card.store, card.note, "", "", "", false);

        cursor.close();
    }

    @Test
    public void TestCursorAdapterWithNote() {
        DBHelper.insertLoyaltyCard(mDatabase, "store", "note", null, null, new BigDecimal("0"), null, "cardId", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), Color.BLACK, 0, null,0);
        LoyaltyCard card = DBHelper.getLoyaltyCard(mDatabase, 1);

        Cursor cursor = DBHelper.getLoyaltyCardCursor(mDatabase);
        cursor.moveToFirst();

        View view = createView(cursor);

        checkView(view, card.store, card.note, "", "", "", false);

        cursor.close();
    }

    @Test
    public void TestCursorAdapterFontSizes() {
        Date date = new Date();
        String dateString = DateFormat.getDateInstance(DateFormat.LONG).format(date);

        DBHelper.insertLoyaltyCard(mDatabase, "store", "note", date, date, new BigDecimal("0"), null, "cardId", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), Color.BLACK, 0, null,0);
        LoyaltyCard card = DBHelper.getLoyaltyCard(mDatabase, 1);

        Cursor cursor = DBHelper.getLoyaltyCardCursor(mDatabase);
        cursor.moveToFirst();

        setFontScale(50);
        View view = createView(cursor);

        checkView(view, card.store, card.note, dateString, dateString, "", true);

        setFontScale(200);
        view = createView(cursor);
        checkView(view, card.store, card.note, dateString, dateString, "", true);

        cursor.close();
    }

    @Test
    public void TestCursorAdapterStarring() {
        assertNotEquals(-1, DBHelper.insertLoyaltyCard(mDatabase, "storeA", "note", null, null, new BigDecimal("0"), null, "cardId", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), Color.BLACK, 0, null,1));
        assertNotEquals(-1, DBHelper.insertLoyaltyCard(mDatabase, "storeB", "note", null, null, new BigDecimal("0"), null, "cardId", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), Color.BLACK, 1, null,1));
        assertNotEquals(-1, DBHelper.insertLoyaltyCard(mDatabase, "storeC", "note", null, null, new BigDecimal("0"), null, "cardId", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), Color.BLACK, 0, null,0));
        assertNotEquals(-1, DBHelper.insertLoyaltyCard(mDatabase, "storeD", "note", null, null, new BigDecimal("0"), null, "cardId", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), Color.BLACK, 1, null,0));

        assertEquals(4, DBHelper.getLoyaltyCardCount(mDatabase));

        Cursor cursor = DBHelper.getLoyaltyCardCursor(mDatabase);
        assertEquals(4, cursor.getCount());

        assertTrue(cursor.moveToFirst());
        LoyaltyCard loyaltyCard = LoyaltyCard.toLoyaltyCard(cursor);
        assertEquals("storeD", loyaltyCard.store);
        View view = createView(cursor);
        ConstraintLayout star = view.findViewById(R.id.star);
        ConstraintLayout archive = view.findViewById(R.id.archivedIcon);
        assertEquals(View.VISIBLE, star.getVisibility());
        assertEquals(View.GONE, archive.getVisibility());

        assertTrue(cursor.moveToNext());
        loyaltyCard = LoyaltyCard.toLoyaltyCard(cursor);
        assertEquals("storeC", loyaltyCard.store);
        view = createView(cursor);
        star = view.findViewById(R.id.star);
        archive = view.findViewById(R.id.archivedIcon);
        assertEquals(View.GONE, star.getVisibility());
        assertEquals(View.GONE, archive.getVisibility());

        assertTrue(cursor.moveToNext());
        loyaltyCard = LoyaltyCard.toLoyaltyCard(cursor);
        assertEquals("storeB", loyaltyCard.store);
        view = createView(cursor);
        star = view.findViewById(R.id.star);
        archive = view.findViewById(R.id.archivedIcon);
        assertEquals(View.VISIBLE, star.getVisibility());
        assertEquals(View.VISIBLE, archive.getVisibility());

        assertTrue(cursor.moveToNext());
        loyaltyCard = LoyaltyCard.toLoyaltyCard(cursor);
        assertEquals("storeA", loyaltyCard.store);
        view = createView(cursor);
        star = view.findViewById(R.id.star);
        archive = view.findViewById(R.id.archivedIcon);
        assertEquals(View.GONE, star.getVisibility());
        assertEquals(View.VISIBLE, archive.getVisibility());

        cursor.close();
    }

    @Test
    public void TestCursorAdapter0Points() {
        DBHelper.insertLoyaltyCard(mDatabase, "store", "", null, null, new BigDecimal("0"), null, "cardId", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), Color.BLACK, 0, null,0);
        LoyaltyCard card = DBHelper.getLoyaltyCard(mDatabase, 1);

        Cursor cursor = DBHelper.getLoyaltyCardCursor(mDatabase);
        cursor.moveToFirst();

        View view = createView(cursor);

        checkView(view, card.store, card.note, "", "", "", false);

        cursor.close();
    }

    @Test
    public void TestCursorAdapter0EUR() {
        DBHelper.insertLoyaltyCard(mDatabase,"store", "", null, null, new BigDecimal("0"), Currency.getInstance("EUR"), "cardId", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), Color.BLACK, 0, null,0);
        LoyaltyCard card = DBHelper.getLoyaltyCard(mDatabase, 1);

        Cursor cursor = DBHelper.getLoyaltyCardCursor(mDatabase);
        cursor.moveToFirst();

        View view = createView(cursor);

        checkView(view, card.store, card.note, "", "", "", false);

        cursor.close();
    }

    @Test
    public void TestCursorAdapter100Points() {
        DBHelper.insertLoyaltyCard(mDatabase, "store", "note", null, null, new BigDecimal("100"), null, "cardId", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), Color.BLACK, 0, null,0);
        LoyaltyCard card = DBHelper.getLoyaltyCard(mDatabase, 1);

        Cursor cursor = DBHelper.getLoyaltyCardCursor(mDatabase);
        cursor.moveToFirst();

        View view = createView(cursor);

        checkView(view, card.store, card.note, "", "", "100 points", false);

        cursor.close();
    }

    @Test
    public void TestCursorAdapter10USD() {
        DBHelper.insertLoyaltyCard(mDatabase, "store", "note", null, null, new BigDecimal("10.00"), Currency.getInstance("USD"), "cardId", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), Color.BLACK, 0, null,0);
        LoyaltyCard card = DBHelper.getLoyaltyCard(mDatabase, 1);

        Cursor cursor = DBHelper.getLoyaltyCardCursor(mDatabase);
        cursor.moveToFirst();

        View view = createView(cursor);

        checkView(view, card.store, card.note, "", "", "$10.00", false);

        cursor.close();
    }
}
