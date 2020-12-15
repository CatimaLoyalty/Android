package protect.card_locker;

import android.app.Activity;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;
import com.google.zxing.BarcodeFormat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowActivity;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class MainActivityTest
{
    private SharedPreferences prefs;

    @Test
    public void initiallyNoLoyaltyCards() throws Exception
    {
        Activity activity = Robolectric.setupActivity(MainActivity.class);
        assertTrue(activity != null);

        TextView helpText = activity.findViewById(R.id.helpText);
        assertEquals(View.VISIBLE, helpText.getVisibility());

        TextView noMatchingCardsText = activity.findViewById(R.id.noMatchingCardsText);
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());

        ListView list = activity.findViewById(R.id.list);
        assertEquals(View.GONE, list.getVisibility());
    }

    @Test
    public void onCreateShouldInflateLayout() throws Exception
    {
        final MainActivity activity = Robolectric.setupActivity(MainActivity.class);

        final Menu menu = shadowOf(activity).getOptionsMenu();
        assertTrue(menu != null);

        // The settings, import/export, groups, search and add button should be present
        assertEquals(menu.size(), 5);
        assertEquals("Search", menu.findItem(R.id.action_search).getTitle().toString());
        assertEquals("Groups", menu.findItem(R.id.action_manage_groups).getTitle().toString());
        assertEquals("Import/Export", menu.findItem(R.id.action_import_export).getTitle().toString());
        assertEquals("About", menu.findItem(R.id.action_about).getTitle().toString());
        assertEquals("Settings", menu.findItem(R.id.action_settings).getTitle().toString());
    }

    @Test
    public void clickAddStartsScan()
    {
        final MainActivity activity = Robolectric.setupActivity(MainActivity.class);

        activity.findViewById(R.id.fabAdd).performClick();

        ShadowActivity shadowActivity = shadowOf(activity);
        assertEquals(shadowActivity.peekNextStartedActivityForResult().intent.getComponent(), new ComponentName(activity, ScanActivity.class));
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
        db.insertLoyaltyCard("store", "note", "cardId", BarcodeFormat.UPC_A.toString(), Color.BLACK, 0);

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
        db.insertLoyaltyCard("storeB", "note", "cardId", BarcodeFormat.UPC_A.toString(), Color.BLACK, 0);
        db.insertLoyaltyCard("storeA", "note", "cardId", BarcodeFormat.UPC_A.toString(), Color.BLACK, 0);
        db.insertLoyaltyCard("storeD", "note", "cardId", BarcodeFormat.UPC_A.toString(), Color.BLACK, 1);
        db.insertLoyaltyCard("storeC", "note", "cardId", BarcodeFormat.UPC_A.toString(), Color.BLACK, 1);

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
    public void testGroups()
    {
        ActivityController activityController = Robolectric.buildActivity(MainActivity.class).create();

        Activity mainActivity = (Activity)activityController.get();
        activityController.start();
        activityController.resume();

        DBHelper db = new DBHelper(mainActivity);

        TabLayout groupTabs = mainActivity.findViewById(R.id.groups);

        // No group tabs by default
        assertEquals(0, groupTabs.getTabCount());

        // Having at least one group should create two tabs: One all and one for each group
        db.insertGroup("One");
        activityController.pause();
        activityController.resume();
        assertEquals(2, groupTabs.getTabCount());
        assertEquals("All", groupTabs.getTabAt(0).getText().toString());
        assertEquals("One", groupTabs.getTabAt(1).getText().toString());

        // Adding another group should have it added to the end
        db.insertGroup("Alphabetical two");
        activityController.pause();
        activityController.resume();
        assertEquals(3, groupTabs.getTabCount());
        assertEquals("All", groupTabs.getTabAt(0).getText().toString());
        assertEquals("One", groupTabs.getTabAt(1).getText().toString());
        assertEquals("Alphabetical two", groupTabs.getTabAt(2).getText().toString());

        // Removing a group should also change the list
        db.deleteGroup("Alphabetical two");
        activityController.pause();
        activityController.resume();
        assertEquals(2, groupTabs.getTabCount());
        assertEquals("All", groupTabs.getTabAt(0).getText().toString());
        assertEquals("One", groupTabs.getTabAt(1).getText().toString());

        // Removing the last group should make the tabs disappear
        db.deleteGroup("One");
        activityController.pause();
        activityController.resume();
        assertEquals(0, groupTabs.getTabCount());
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
        TabLayout groupTabs = mainActivity.findViewById(R.id.groups);

        DBHelper db = new DBHelper(mainActivity);
        db.insertLoyaltyCard("The First Store", "Initial note", "cardId", BarcodeFormat.UPC_A.toString(), Color.BLACK, 0);
        db.insertLoyaltyCard("The Second Store", "Secondary note", "cardId", BarcodeFormat.UPC_A.toString(), Color.BLACK, 0);

        db.insertGroup("Group one");
        List<Group> groups = new ArrayList<>();
        groups.add(db.getGroup("Group one"));
        db.setLoyaltyCardGroups(1, groups);

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(2, list.getCount());

        mainActivity.filter = "store";

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(2, list.getCount());

        // Switch to Group one
        groupTabs.selectTab(groupTabs.getTabAt(1));

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getCount());

        // Switch back to all groups
        groupTabs.selectTab(groupTabs.getTabAt(0));

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(2, list.getCount());

        mainActivity.filter = "first";

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getCount());

        // Switch to Group one
        groupTabs.selectTab(groupTabs.getTabAt(1));

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getCount());

        // Switch back to all groups
        groupTabs.selectTab(groupTabs.getTabAt(0));

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getCount());

        mainActivity.filter = "initial";

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getCount());

        // Switch to Group one
        groupTabs.selectTab(groupTabs.getTabAt(1));

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getCount());

        // Switch back to all groups
        groupTabs.selectTab(groupTabs.getTabAt(0));

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getCount());

        mainActivity.filter = "second";

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getCount());

        // Switch to Group one
        groupTabs.selectTab(groupTabs.getTabAt(1));

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.VISIBLE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(0, list.getCount());

        // Switch back to all groups
        groupTabs.selectTab(groupTabs.getTabAt(0));

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getCount());

        mainActivity.filter = "company";

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.VISIBLE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(0, list.getCount());

        // Switch to Group one
        groupTabs.selectTab(groupTabs.getTabAt(1));

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.VISIBLE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(0, list.getCount());

        // Switch back to all groups
        groupTabs.selectTab(groupTabs.getTabAt(0));

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.VISIBLE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(0, list.getCount());

        mainActivity.filter = "";

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(2, list.getCount());

        // Switch to Group one
        groupTabs.selectTab(groupTabs.getTabAt(1));

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getCount());

        // Switch back to all groups
        groupTabs.selectTab(groupTabs.getTabAt(0));

        assertEquals(View.GONE, helpText.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(2, list.getCount());
    }
}