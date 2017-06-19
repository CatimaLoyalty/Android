package protect.card_locker;

import android.app.Activity;
import android.database.Cursor;
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

    private void checkView(final View view, final String store, final String cardId)
    {
        final TextView storeField = (TextView) view.findViewById(R.id.store);
        assertEquals(store, storeField.getText().toString());

        final TextView cardIdField = (TextView) view.findViewById(R.id.cardId);
        assertEquals(cardId, cardIdField.getText().toString());
    }


    @Test
    public void TestCursorAdapterEmptyNote()
    {
        db.insertLoyaltyCard("store", "", "cardId", BarcodeFormat.UPC_A.toString());
        LoyaltyCard card = db.getLoyaltyCard(1);

        Cursor cursor = db.getLoyaltyCardCursor();
        cursor.moveToFirst();

        View view = createView(cursor);

        final String cardIdLabel = activity.getResources().getString(R.string.cardId);
        final String cardIdFormat = activity.getResources().getString(R.string.cardIdFormat);
        String cardIdText = String.format(cardIdFormat, cardIdLabel, card.cardId);

        checkView(view, card.store, cardIdText);
    }

    @Test
    public void TestCursorAdapterWithNote()
    {
        db.insertLoyaltyCard("store", "note", "cardId", BarcodeFormat.UPC_A.toString());
        LoyaltyCard card = db.getLoyaltyCard(1);

        Cursor cursor = db.getLoyaltyCardCursor();
        cursor.moveToFirst();

        View view = createView(cursor);

        final String storeNameAndNoteFormat = activity.getResources().getString(R.string.storeNameAndNoteFormat);
        String storeAndNoteText = String.format(storeNameAndNoteFormat, card.store, card.note);

        final String cardIdLabel = activity.getResources().getString(R.string.cardId);
        final String cardIdFormat = activity.getResources().getString(R.string.cardIdFormat);
        String cardIdText = String.format(cardIdFormat, cardIdLabel, card.cardId);

        checkView(view, storeAndNoteText, cardIdText);
    }
}
