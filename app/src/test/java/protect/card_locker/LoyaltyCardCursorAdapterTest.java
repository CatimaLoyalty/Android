package protect.card_locker;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

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

    private void checkView(final View view, final String store, final String note, boolean checkFontSizes)
    {
        final TextView storeField = view.findViewById(R.id.store);
        final TextView noteField = view.findViewById(R.id.note);

        if(checkFontSizes)
        {
            int storeFontSize = settings.getInt(activity.getResources().getString(R.string.settings_key_card_title_list_font_size), 0);
            int noteFontSize = settings.getInt(activity.getResources().getString(R.string.settings_key_card_note_list_font_size), 0);

            assertEquals(storeFontSize, (int)storeField.getTextSize());
            assertEquals(noteFontSize, (int)noteField.getTextSize());
        }

        assertEquals(store, storeField.getText().toString());
        if(note.isEmpty() == false)
        {
            assertEquals(View.VISIBLE, noteField.getVisibility());
            assertEquals(note, noteField.getText().toString());
        }
        else
        {
            assertEquals(View.GONE, noteField.getVisibility());
        }
    }


    @Test
    public void TestCursorAdapterEmptyNote()
    {
        db.insertLoyaltyCard("store", "", "cardId", BarcodeFormat.UPC_A.toString(), Color.BLACK, Color.WHITE);
        LoyaltyCard card = db.getLoyaltyCard(1);

        Cursor cursor = db.getLoyaltyCardCursor();
        cursor.moveToFirst();

        View view = createView(cursor);

        checkView(view, card.store, card.note, false);
    }

    @Test
    public void TestCursorAdapterWithNote()
    {
        db.insertLoyaltyCard("store", "note", "cardId", BarcodeFormat.UPC_A.toString(), Color.BLACK, Color.WHITE);
        LoyaltyCard card = db.getLoyaltyCard(1);

        Cursor cursor = db.getLoyaltyCardCursor();
        cursor.moveToFirst();

        View view = createView(cursor);

        checkView(view, card.store, card.note, false);
    }

    @Test
    public void TestCursorAdapterFontSizes()
    {
        db.insertLoyaltyCard("store", "note", "cardId", BarcodeFormat.UPC_A.toString(), Color.BLACK, Color.WHITE);
        LoyaltyCard card = db.getLoyaltyCard(1);

        Cursor cursor = db.getLoyaltyCardCursor();
        cursor.moveToFirst();

        setFontSizes(1, 2);
        View view = createView(cursor);
        checkView(view, card.store, card.note, true);

        setFontSizes(30, 31);
        view = createView(cursor);
        checkView(view, card.store, card.note, true);
    }
}
