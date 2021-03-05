package protect.card_locker;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.Currency;
import java.util.Date;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class LoyaltyCardCursorAdapterTest
{
    private Activity activity;
    private DBHelper db;
    private SharedPreferences settings;

    @Before
    public void setUp()
    {
        activity = Robolectric.setupActivity(MainActivity.class);
        db = new DBHelper(activity);
        settings = PreferenceManager.getDefaultSharedPreferences(activity);
    }

    private void setFontSizes(int storeFontSize, int noteFontSize)
    {
        settings.edit()
            .putInt(activity.getResources().getString(R.string.settings_key_card_title_list_font_size), storeFontSize)
            .putInt(activity.getResources().getString(R.string.settings_key_card_note_list_font_size), noteFontSize)
            .apply();
    }

    private View createView(Cursor cursor)
    {
        LoyaltyCardCursorAdapter adapter = new LoyaltyCardCursorAdapter(activity.getApplicationContext(), cursor);

        View view = adapter.newView(activity.getApplicationContext(), cursor, null);
        adapter.bindView(view, activity.getApplicationContext(), cursor);

        return view;
    }

    private void checkView(final View view, final String store, final String note, final String expiry, final String balance, boolean checkFontSizes)
    {
        final TextView storeField = view.findViewById(R.id.store);
        final TextView noteField = view.findViewById(R.id.note);
        final TextView expiryField = view.findViewById(R.id.expiry);
        final TextView balanceField = view.findViewById(R.id.balance);

        if(checkFontSizes)
        {
            int storeFontSize = settings.getInt(activity.getResources().getString(R.string.settings_key_card_title_list_font_size), 0);
            int noteFontSize = settings.getInt(activity.getResources().getString(R.string.settings_key_card_note_list_font_size), 0);

            assertEquals(storeFontSize, (int)storeField.getTextSize());
            assertEquals(noteFontSize, (int)noteField.getTextSize());
            assertEquals(noteFontSize, (int)expiryField.getTextSize());
        }

        assertEquals(store, storeField.getText().toString());
        if(!note.isEmpty())
        {
            assertEquals(View.VISIBLE, noteField.getVisibility());
            assertEquals(note, noteField.getText().toString());
        }
        else
        {
            assertEquals(View.GONE, noteField.getVisibility());
        }

        if(!expiry.isEmpty())
        {
            assertEquals(View.VISIBLE, expiryField.getVisibility());
            assertEquals(expiry, expiryField.getText().toString());
        }
        else
        {
            assertEquals(View.GONE, expiryField.getVisibility());
        }

        if(!balance.isEmpty())
        {
            assertEquals(View.VISIBLE, balanceField.getVisibility());
            assertEquals(balance, balanceField.getText().toString());
        }
        else
        {
            assertEquals(View.GONE, balanceField.getVisibility());
        }
    }


    @Test
    public void TestCursorAdapterEmptyNote()
    {
        db.insertLoyaltyCard("store", "", null, new BigDecimal("0"), null, "cardId", BarcodeFormat.UPC_A.toString(), Color.BLACK, 0);
        LoyaltyCard card = db.getLoyaltyCard(1);

        Cursor cursor = db.getLoyaltyCardCursor();
        cursor.moveToFirst();

        View view = createView(cursor);

        checkView(view, card.store, card.note, "", "",false);

        cursor.close();
    }

    @Test
    public void TestCursorAdapterWithNote()
    {
        db.insertLoyaltyCard("store", "note", null, new BigDecimal("0"), null, "cardId", BarcodeFormat.UPC_A.toString(), Color.BLACK, 0);
        LoyaltyCard card = db.getLoyaltyCard(1);

        Cursor cursor = db.getLoyaltyCardCursor();
        cursor.moveToFirst();

        View view = createView(cursor);

        checkView(view, card.store, card.note, "", "",false);

        cursor.close();
    }

    @Test
    public void TestCursorAdapterFontSizes()
    {
        final Context context = ApplicationProvider.getApplicationContext();
        Date expiryDate = new Date();
        String dateString = context.getString(R.string.expiryStateSentence, DateFormat.getDateInstance(DateFormat.LONG).format(expiryDate));

        db.insertLoyaltyCard("store", "note", expiryDate, new BigDecimal("0"), null, "cardId", BarcodeFormat.UPC_A.toString(), Color.BLACK, 0);
        LoyaltyCard card = db.getLoyaltyCard(1);

        Cursor cursor = db.getLoyaltyCardCursor();
        cursor.moveToFirst();

        setFontSizes(1, 2);
        View view = createView(cursor);

        checkView(view, card.store, card.note, dateString, "", true);

        setFontSizes(30, 31);
        view = createView(cursor);
        checkView(view, card.store, card.note, dateString, "",true);

        cursor.close();
    }

    @Test
    public void TestCursorAdapterStarring()
    {
        db.insertLoyaltyCard("storeA", "note", null, new BigDecimal("0"), null, "cardId", BarcodeFormat.UPC_A.toString(), Color.BLACK, 0);
        db.insertLoyaltyCard("storeB", "note", null, new BigDecimal("0"), null, "cardId", BarcodeFormat.UPC_A.toString(), Color.BLACK, 1);
        db.insertLoyaltyCard("storeC", "note", null, new BigDecimal("0"), null, "cardId", BarcodeFormat.UPC_A.toString(), Color.BLACK, 1);

        Cursor cursor = db.getLoyaltyCardCursor();
        cursor.moveToFirst();
        View view = createView(cursor);
        ImageView star = view.findViewById(R.id.star);
        assertEquals(View.VISIBLE, star.getVisibility());

        cursor.moveToNext();
        view = createView(cursor);
        star = view.findViewById(R.id.star);
        assertEquals(View.VISIBLE, star.getVisibility());

        cursor.moveToNext();
        view = createView(cursor);
        star = view.findViewById(R.id.star);
        assertEquals(View.GONE, star.getVisibility());

        cursor.close();
    }

    @Test
    public void TestCursorAdapter0Points()
    {
        db.insertLoyaltyCard("store", "", null, new BigDecimal("0"), null, "cardId", BarcodeFormat.UPC_A.toString(), Color.BLACK, 0);
        LoyaltyCard card = db.getLoyaltyCard(1);

        Cursor cursor = db.getLoyaltyCardCursor();
        cursor.moveToFirst();

        View view = createView(cursor);

        checkView(view, card.store, card.note, "", "",false);

        cursor.close();
    }

    @Test
    public void TestCursorAdapter0EUR()
    {
        db.insertLoyaltyCard("store", "", null, new BigDecimal("0"), Currency.getInstance("EUR"), "cardId", BarcodeFormat.UPC_A.toString(), Color.BLACK, 0);
        LoyaltyCard card = db.getLoyaltyCard(1);

        Cursor cursor = db.getLoyaltyCardCursor();
        cursor.moveToFirst();

        View view = createView(cursor);

        checkView(view, card.store, card.note, "", "",false);

        cursor.close();
    }

    @Test
    public void TestCursorAdapter100Points()
    {
        db.insertLoyaltyCard("store", "note", null, new BigDecimal("100"), null, "cardId", BarcodeFormat.UPC_A.toString(), Color.BLACK, 0);
        LoyaltyCard card = db.getLoyaltyCard(1);

        Cursor cursor = db.getLoyaltyCardCursor();
        cursor.moveToFirst();

        View view = createView(cursor);

        checkView(view, card.store, card.note, "", "Balance: 100 points",false);

        cursor.close();
    }

    @Test
    public void TestCursorAdapter10USD()
    {
        db.insertLoyaltyCard("store", "note", null, new BigDecimal("10.00"), Currency.getInstance("USD"), "cardId", BarcodeFormat.UPC_A.toString(), Color.BLACK, 0);
        LoyaltyCard card = db.getLoyaltyCard(1);

        Cursor cursor = db.getLoyaltyCardCursor();
        cursor.moveToFirst();

        View view = createView(cursor);

        checkView(view, card.store, card.note, "", "Balance: $10.00",false);

        cursor.close();
    }
}
