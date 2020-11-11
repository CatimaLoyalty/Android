package protect.card_locker;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import com.google.zxing.BarcodeFormat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class MainActivityTest
{
    private SharedPreferences prefs;

    @Test
    public void initiallyNoLoyaltyCards() {
        Activity activity = Robolectric.setupActivity(MainActivity.class);
        assertNotNull(activity);

        TextView helpText = activity.findViewById(R.id.helpText);
        assertEquals(View.VISIBLE, helpText.getVisibility());

        TextView noMatchingCardsText = activity.findViewById(R.id.noMatchingCardsText);
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());

        ListView list = activity.findViewById(R.id.list);
        assertEquals(View.GONE, list.getVisibility());
    }

    @Test
    public void onCreateShouldInflateLayout() {
        final MainActivity activity = Robolectric.setupActivity(MainActivity.class);

        final Menu menu = shadowOf(activity).getOptionsMenu();
        assertNotNull(menu);

        // The settings, search and add button should be present
        assertEquals(menu.size(), 4);
        assertEquals("Search", menu.findItem(R.id.action_search).getTitle().toString());
        assertEquals("Import/Export", menu.findItem(R.id.action_import_export).getTitle().toString());
        assertEquals("About", menu.findItem(R.id.action_about).getTitle().toString());
        assertEquals("Settings", menu.findItem(R.id.action_settings).getTitle().toString());
    }

    @Test
    public void clickAddLaunchesLoyaltyCardEditActivity()
    {
        final MainActivity activity = Robolectric.setupActivity(MainActivity.class);

        activity.findViewById(R.id.fabAdd).performClick();

        Intent intent = shadowOf(activity).peekNextStartedActivityForResult().intent;

        assertEquals(new ComponentName(activity, LoyaltyCardEditActivity.class), intent.getComponent());
        assertNull(intent.getExtras());
    }

    @Test
    public void addOneLoyaltyCard()
    {
        ActivityController activityController = Robolectric.buildActivity(MainActivity.class).create();

        Activity mainActivity = (Activity)activityController.get();
        activityController.start();
        activityController.resume();

        TextView helpText = mainActivity.findViewById(R.id.helpText);
        TextView noMatchingCardsText = mainActivity.findViewById(R.id.noMatchingCardsText);
        ListView list = mainActivity.findViewById(R.id.list);

        assertEquals(0, list.getCount());

        DBHelper db = new DBHelper(mainActivity);
        db.insertLoyaltyCard("store", "note", "cardId", BarcodeFormat.UPC_A.toString(), Color.BLACK, Color.WHITE, 0);

        assertEquals(View.VISIBLE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.GONE, list.getVisibility());

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getAdapter().getCount());
        Cursor cursor = (Cursor)list.getAdapter().getItem(0);
        assertNotNull(cursor);
    }

    @Test
    public void addFourLoyaltyCardsTwoStarred()  // Main screen showing starred cards on top correctly
    {
        ActivityController activityController = Robolectric.buildActivity(MainActivity.class).create();

        Activity mainActivity = (Activity)activityController.get();
        activityController.start();
        activityController.resume();

        TextView helpText = mainActivity.findViewById(R.id.helpText);
        TextView noMatchingCardsText = mainActivity.findViewById(R.id.noMatchingCardsText);
        ListView list = mainActivity.findViewById(R.id.list);

        assertEquals(0, list.getCount());

        DBHelper db = new DBHelper(mainActivity);
        db.insertLoyaltyCard("storeB", "note", "cardId", BarcodeFormat.UPC_A.toString(), Color.BLACK, Color.WHITE, 0);
        db.insertLoyaltyCard("storeA", "note", "cardId", BarcodeFormat.UPC_A.toString(), Color.BLACK, Color.WHITE, 0);
        db.insertLoyaltyCard("storeD", "note", "cardId", BarcodeFormat.UPC_A.toString(), Color.BLACK, Color.WHITE, 1);
        db.insertLoyaltyCard("storeC", "note", "cardId", BarcodeFormat.UPC_A.toString(), Color.BLACK, Color.WHITE, 1);

        assertEquals(View.VISIBLE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.GONE, list.getVisibility());

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(4, list.getAdapter().getCount());
        Cursor cursor = (Cursor)list.getAdapter().getItem(0);
        assertNotNull(cursor);
        assertEquals("storeC",cursor.getString(cursor.getColumnIndex("store")));

        cursor = (Cursor)list.getAdapter().getItem(1);
        assertNotNull(cursor);
        assertEquals("storeD",cursor.getString(cursor.getColumnIndex("store")));

        cursor = (Cursor)list.getAdapter().getItem(2);
        assertNotNull(cursor);
        assertEquals("storeA",cursor.getString(cursor.getColumnIndex("store")));

        cursor = (Cursor)list.getAdapter().getItem(3);
        assertNotNull(cursor);
        assertEquals("storeB",cursor.getString(cursor.getColumnIndex("store")));
    }

    @Test
    public void testFiltering()
    {
        ActivityController activityController = Robolectric.buildActivity(MainActivity.class).create();

        MainActivity mainActivity = (MainActivity)activityController.get();
        activityController.start();
        activityController.resume();

        TextView helpText = mainActivity.findViewById(R.id.helpText);
        TextView noMatchingCardsText = mainActivity.findViewById(R.id.noMatchingCardsText);
        ListView list = mainActivity.findViewById(R.id.list);

        DBHelper db = new DBHelper(mainActivity);
        db.insertLoyaltyCard("The First Store", "Initial note", "cardId", BarcodeFormat.UPC_A.toString(), Color.BLACK, Color.WHITE, 0);
        db.insertLoyaltyCard("The Second Store", "Secondary note", "cardId", BarcodeFormat.UPC_A.toString(), Color.BLACK, Color.WHITE, 0);

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(2, list.getCount());

        mainActivity.mFilter = "store";

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(2, list.getCount());

        mainActivity.mFilter = "first";

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getCount());

        mainActivity.mFilter = "initial";

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getCount());

        mainActivity.mFilter = "second";

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getCount());

        mainActivity.mFilter = "company";

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.VISIBLE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(0, list.getCount());

        mainActivity.mFilter = "";

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(2, list.getCount());
    }
}