package protect.card_locker;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Color;
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
@Config(constants = BuildConfig.class, sdk = 23)
public class LoyaltyCardCursorAdapterTest
{
    private Activity activity;
    private DBHelper db;

    @Before
    public void setUp()
    {
        activity = Robolectric.setupActivity(MainActivity.class);
        db = new DBHelper(activity);
    }

    private View createView(Cursor cursor)
    {
        LoyaltyCardCursorAdapter adapter = new LoyaltyCardCursorAdapter(activity.getApplicationContext(), cursor);

        View view = adapter.newView(activity.getApplicationContext(), cursor, null);
        adapter.bindView(view, activity.getApplicationContext(), cursor);

        return view;
    }

    private void checkView(final View view, final String store, final String note)
    {
        final TextView storeField = view.findViewById(R.id.store);
        assertEquals(store, storeField.getText().toString());

        final TextView noteField = view.findViewById(R.id.note);
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

        checkView(view, card.store, card.note);
    }

    @Test
    public void TestCursorAdapterWithNote()
    {
        db.insertLoyaltyCard("store", "note", "cardId", BarcodeFormat.UPC_A.toString(), Color.BLACK, Color.WHITE);
        LoyaltyCard card = db.getLoyaltyCard(1);

        Cursor cursor = db.getLoyaltyCardCursor();
        cursor.moveToFirst();

        View view = createView(cursor);

        checkView(view, card.store, card.note);
    }
}
