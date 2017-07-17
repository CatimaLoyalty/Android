package protect.card_locker;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23)
public class MainActivityTest
{
    private SharedPreferences prefs;

    @Before
    public void setUp()
    {
        // Assume that this is not the first launch
        prefs = RuntimeEnvironment.application.getSharedPreferences("protect.card_locker", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("firstrun", false).commit();
    }

    @Test
    public void initiallyNoLoyaltyCards() throws Exception
    {
        Activity activity = Robolectric.setupActivity(MainActivity.class);
        assertTrue(activity != null);

        TextView helpText = (TextView)activity.findViewById(R.id.helpText);
        assertEquals(View.VISIBLE, helpText.getVisibility());

        ListView list = (ListView)activity.findViewById(R.id.list);
        assertEquals(View.GONE, list.getVisibility());
    }

    @Test
    public void onCreateShouldInflateLayout() throws Exception
    {
        final MainActivity activity = Robolectric.setupActivity(MainActivity.class);

        final Menu menu = shadowOf(activity).getOptionsMenu();
        assertTrue(menu != null);

        // The settings and add button should be present
        assertEquals(menu.size(), 4);

        assertEquals("Add", menu.findItem(R.id.action_add).getTitle().toString());
        assertEquals("Import/Export", menu.findItem(R.id.action_import_export).getTitle().toString());
        assertEquals("Start Intro", menu.findItem(R.id.action_intro).getTitle().toString());
        assertEquals("About", menu.findItem(R.id.action_about).getTitle().toString());
    }

    @Test
    public void clickAddLaunchesLoyaltyCardViewActivity()
    {
        final MainActivity activity = Robolectric.setupActivity(MainActivity.class);

        shadowOf(activity).clickMenuItem(R.id.action_add);

        Intent intent = shadowOf(activity).peekNextStartedActivityForResult().intent;

        assertEquals(new ComponentName(activity, LoyaltyCardViewActivity.class), intent.getComponent());
        assertNull(intent.getExtras());
    }

    @Test
    public void addOneLoyaltyCard()
    {
        ActivityController activityController = Robolectric.buildActivity(MainActivity.class).create();

        Activity mainActivity = (Activity)activityController.get();
        activityController.start();
        activityController.resume();

        TextView helpText = (TextView)mainActivity.findViewById(R.id.helpText);
        ListView list = (ListView)mainActivity.findViewById(R.id.list);

        assertEquals(0, list.getCount());

        DBHelper db = new DBHelper(mainActivity);
        db.insertLoyaltyCard("store", "note", "cardId", BarcodeFormat.UPC_A.toString());

        assertEquals(View.VISIBLE, helpText.getVisibility());
        assertEquals(View.GONE, list.getVisibility());

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getAdapter().getCount());
        Cursor cursor = (Cursor)list.getAdapter().getItem(0);
        assertNotNull(cursor);
    }

    @Test
    public void testFirstRunStartsIntro()
    {
        prefs.edit().remove("firstrun").commit();

        ActivityController controller = Robolectric.buildActivity(MainActivity.class).create();
        Activity activity = (Activity)controller.get();

        assertTrue(activity.isFinishing() == false);

        Intent next = shadowOf(activity).getNextStartedActivity();

        ComponentName componentName = next.getComponent();
        String name = componentName.flattenToShortString();
        assertEquals("protect.card_locker/.intro.IntroActivity", name);

        Bundle extras = next.getExtras();
        assertNull(extras);

        assertEquals(false, prefs.getBoolean("firstrun", true));
    }
}