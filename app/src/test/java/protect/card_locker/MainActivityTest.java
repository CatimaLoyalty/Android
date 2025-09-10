package protect.card_locker;

import static android.os.Looper.getMainLooper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.zxing.BarcodeFormat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowActivity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class MainActivityTest {
    private SharedPreferences prefs;

    @Test
    public void initiallyNoLoyaltyCards() {
        Activity activity = Robolectric.setupActivity(MainActivity.class);
        assertNotNull(activity);

        LinearLayout helpSection = activity.findViewById(R.id.helpSection);
        assertEquals(View.VISIBLE, helpSection.getVisibility());

        TextView noMatchingCardsText = activity.findViewById(R.id.noMatchingCardsText);
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());

        RecyclerView list = activity.findViewById(R.id.list);
        assertEquals(View.GONE, list.getVisibility());
    }

    @Test
    public void onCreateShouldInflateLayout() {
        final MainActivity activity = Robolectric.setupActivity(MainActivity.class);

        final Menu menu = shadowOf(activity).getOptionsMenu();
        assertNotNull(menu);

        // The settings, import/export, groups, search and add button should be present
        assertEquals(menu.size(), 7);
        assertEquals("Search", menu.findItem(R.id.action_search).getTitle().toString());
        assertEquals("Sort", menu.findItem(R.id.action_sort).getTitle().toString());
        assertEquals("Display options", menu.findItem(R.id.action_display_options).getTitle().toString());
        assertEquals("Groups", menu.findItem(R.id.action_manage_groups).getTitle().toString());
        assertEquals("Import/export", menu.findItem(R.id.action_import_export).getTitle().toString());
        assertEquals("About", menu.findItem(R.id.action_about).getTitle().toString());
        assertEquals("Settings", menu.findItem(R.id.action_settings).getTitle().toString());
    }

    @Test
    public void clickAddStartsScan() {
        final MainActivity activity = Robolectric.setupActivity(MainActivity.class);

        activity.findViewById(R.id.fabAdd).performClick();

        ShadowActivity shadowActivity = shadowOf(activity);
        assertEquals(shadowActivity.peekNextStartedActivityForResult().intent.getComponent(), new ComponentName(activity, ScanActivity.class));
    }

    @Test
    public void addOneLoyaltyCard() {
        ActivityController activityController = Robolectric.buildActivity(MainActivity.class).create();

        Activity mainActivity = (Activity) activityController.get();
        activityController.start();
        activityController.resume();

        LinearLayout helpSection = mainActivity.findViewById(R.id.helpSection);
        TextView noMatchingCardsText = mainActivity.findViewById(R.id.noMatchingCardsText);
        RecyclerView list = mainActivity.findViewById(R.id.list);

        assertEquals(0, list.getAdapter().getItemCount());

        SQLiteDatabase database = TestHelpers.getEmptyDb(mainActivity).getWritableDatabase();
        DBHelper.insertLoyaltyCard(database, "store", "note", null, null, new BigDecimal("0"), null, "cardId", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), Color.BLACK, 0, null,0);

        assertEquals(View.VISIBLE, helpSection.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.GONE, list.getVisibility());

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpSection.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getAdapter().getItemCount());

        database.close();
    }

    @Test
    public void addFourLoyaltyCardsTwoStarred()  // Main screen showing starred cards on top correctly
    {
        ActivityController activityController = Robolectric.buildActivity(MainActivity.class).create();

        Activity mainActivity = (Activity) activityController.get();
        activityController.start();
        activityController.resume();
        activityController.visible();

        LinearLayout helpSection = mainActivity.findViewById(R.id.helpSection);
        TextView noMatchingCardsText = mainActivity.findViewById(R.id.noMatchingCardsText);
        RecyclerView list = mainActivity.findViewById(R.id.list);

        assertEquals(0, list.getAdapter().getItemCount());

        SQLiteDatabase database = TestHelpers.getEmptyDb(mainActivity).getWritableDatabase();
        DBHelper.insertLoyaltyCard(database, "storeB", "note", null, null, new BigDecimal("0"), null, "cardId", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), Color.BLACK, 0, null,0);
        DBHelper.insertLoyaltyCard(database, "storeA", "note", null, null, new BigDecimal("0"), null, "cardId", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), Color.BLACK, 0, null,0);
        DBHelper.insertLoyaltyCard(database, "storeD", "note", null, null, new BigDecimal("0"), null, "cardId", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), Color.BLACK, 1, null,0);
        DBHelper.insertLoyaltyCard(database, "storeC", "note", null, null, new BigDecimal("0"), null, "cardId", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), Color.BLACK, 1, null,0);

        assertEquals(View.VISIBLE, helpSection.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.GONE, list.getVisibility());

        activityController.pause();
        activityController.resume();
        activityController.visible();

        assertEquals(View.GONE, helpSection.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(4, list.getAdapter().getItemCount());

        // Make sure there is enough space to render all
        list.measure(0, 0);
        list.layout(0, 0, 100, 1000);

        assertEquals("storeC", ((TextView) list.findViewHolderForAdapterPosition(0).itemView.findViewById(R.id.thumbnail_text)).getText());
        assertEquals("storeD", ((TextView) list.findViewHolderForAdapterPosition(1).itemView.findViewById(R.id.thumbnail_text)).getText());
        assertEquals("storeA", ((TextView) list.findViewHolderForAdapterPosition(2).itemView.findViewById(R.id.thumbnail_text)).getText());
        assertEquals("storeB", ((TextView) list.findViewHolderForAdapterPosition(3).itemView.findViewById(R.id.thumbnail_text)).getText());

        database.close();
    }

    @Test
    public void testGroups() {
        ActivityController activityController = Robolectric.buildActivity(MainActivity.class).create();

        Activity mainActivity = (Activity) activityController.get();
        activityController.start();
        activityController.resume();

        SQLiteDatabase database = TestHelpers.getEmptyDb(mainActivity).getWritableDatabase();

        TabLayout groupTabs = mainActivity.findViewById(R.id.groups);

        // No group tabs by default
        assertEquals(0, groupTabs.getTabCount());

        // Having at least one group should create two tabs: One all and one for each group
        DBHelper.insertGroup(database, "One");
        activityController.pause();
        activityController.resume();
        assertEquals(2, groupTabs.getTabCount());
        assertEquals("All", groupTabs.getTabAt(0).getText().toString());
        assertEquals("One", groupTabs.getTabAt(1).getText().toString());

        // Adding another group should have it added to the end
        DBHelper.insertGroup(database, "Alphabetical two");
        activityController.pause();
        activityController.resume();
        assertEquals(3, groupTabs.getTabCount());
        assertEquals("All", groupTabs.getTabAt(0).getText().toString());
        assertEquals("One", groupTabs.getTabAt(1).getText().toString());
        assertEquals("Alphabetical two", groupTabs.getTabAt(2).getText().toString());

        // Removing a group should also change the list
        DBHelper.deleteGroup(database, "Alphabetical two");
        activityController.pause();
        activityController.resume();
        assertEquals(2, groupTabs.getTabCount());
        assertEquals("All", groupTabs.getTabAt(0).getText().toString());
        assertEquals("One", groupTabs.getTabAt(1).getText().toString());

        // Removing the last group should make the tabs disappear
        DBHelper.deleteGroup(database, "One");
        activityController.pause();
        activityController.resume();
        assertEquals(0, groupTabs.getTabCount());

        database.close();
    }

    @Test
    public void testFiltering() {
        // FIXME: This test directly sets mFilter instead of using mSearchView, making it not test the search flow correctly
        // It may falsely succeed or fail, but we're leaving it here so we at least test something instead of nothing
        ActivityController activityController = Robolectric.buildActivity(MainActivity.class).create();

        MainActivity mainActivity = (MainActivity) activityController.get();

        activityController.start();
        activityController.resume();

        LinearLayout helpSection = mainActivity.findViewById(R.id.helpSection);
        TextView noMatchingCardsText = mainActivity.findViewById(R.id.noMatchingCardsText);
        RecyclerView list = mainActivity.findViewById(R.id.list);
        TabLayout groupTabs = mainActivity.findViewById(R.id.groups);

        SQLiteDatabase database = TestHelpers.getEmptyDb(mainActivity).getWritableDatabase();
        DBHelper.insertLoyaltyCard(database, "The First Store", "Initial note", null, null, new BigDecimal("0"), null, "cardId", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), Color.BLACK, 0, null,0);
        DBHelper.insertLoyaltyCard(database, "The Second Store", "Secondary note", null, null, new BigDecimal("0"), null, "cardId", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), Color.BLACK, 0, null,0);

        DBHelper.insertGroup(database, "Group one");
        List<Group> groups = new ArrayList<>();
        groups.add(DBHelper.getGroup(database, "Group one"));
        DBHelper.setLoyaltyCardGroups(database, 1, groups);

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpSection.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(2, list.getAdapter().getItemCount());

        mainActivity.mFilter = "store";

        activityController.pause();
        activityController.resume();

        Configuration configuration = mainActivity.getResources().getConfiguration();
        configuration.orientation = Configuration.ORIENTATION_LANDSCAPE;
        mainActivity.onConfigurationChanged(configuration);

        configuration.orientation = Configuration.ORIENTATION_PORTRAIT;
        mainActivity.onConfigurationChanged(configuration);

        configuration.orientation = Configuration.ORIENTATION_LANDSCAPE;
        mainActivity.onConfigurationChanged(configuration);

        assertEquals(View.GONE, helpSection.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(2, list.getAdapter().getItemCount());

        // Switch to Group one
        groupTabs.selectTab(groupTabs.getTabAt(1));

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpSection.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getAdapter().getItemCount());

        // Switch back to all groups
        groupTabs.selectTab(groupTabs.getTabAt(0));

        assertEquals(View.GONE, helpSection.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(2, list.getAdapter().getItemCount());

        mainActivity.mFilter = "first";

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpSection.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getAdapter().getItemCount());

        // Switch to Group one
        groupTabs.selectTab(groupTabs.getTabAt(1));

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpSection.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getAdapter().getItemCount());

        // Switch back to all groups
        groupTabs.selectTab(groupTabs.getTabAt(0));

        assertEquals(View.GONE, helpSection.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getAdapter().getItemCount());

        mainActivity.mFilter = "initial";

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpSection.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getAdapter().getItemCount());

        // Switch to Group one
        groupTabs.selectTab(groupTabs.getTabAt(1));

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpSection.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getAdapter().getItemCount());

        // Switch back to all groups
        groupTabs.selectTab(groupTabs.getTabAt(0));

        assertEquals(View.GONE, helpSection.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getAdapter().getItemCount());

        mainActivity.mFilter = "second";

        configuration.orientation = Configuration.ORIENTATION_LANDSCAPE;
        mainActivity.onConfigurationChanged(configuration);

        configuration.orientation = Configuration.ORIENTATION_PORTRAIT;
        mainActivity.onConfigurationChanged(configuration);

        activityController.pause();
        activityController.resume();

        assertEquals(View.GONE, helpSection.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getAdapter().getItemCount());

        // Switch to Group one
        groupTabs.selectTab(groupTabs.getTabAt(1));

        activityController.pause();
        activityController.resume();

        shadowOf(getMainLooper()).idle();

        assertEquals(View.GONE, helpSection.getVisibility());
        assertEquals(View.VISIBLE, noMatchingCardsText.getVisibility());
        assertEquals(View.GONE, list.getVisibility());

        assertEquals(0, list.getAdapter().getItemCount());

        // Switch back to all groups
        groupTabs.selectTab(groupTabs.getTabAt(0));

        assertEquals(View.GONE, helpSection.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getAdapter().getItemCount());

        mainActivity.mFilter = "company";

        // Rotate to landscape (right)
        configuration.orientation = Configuration.ORIENTATION_LANDSCAPE;
        mainActivity.onConfigurationChanged(configuration);

        activityController.pause();
        activityController.resume();

        shadowOf(getMainLooper()).idle();

        assertEquals(View.GONE, helpSection.getVisibility());
        assertEquals(View.VISIBLE, noMatchingCardsText.getVisibility());
        assertEquals(View.GONE, list.getVisibility());

        assertEquals(0, list.getAdapter().getItemCount());

        // Switch to Group one
        groupTabs.selectTab(groupTabs.getTabAt(1));

        activityController.pause();
        activityController.resume();

        shadowOf(getMainLooper()).idle();

        assertEquals(View.GONE, helpSection.getVisibility());
        assertEquals(View.VISIBLE, noMatchingCardsText.getVisibility());
        assertEquals(View.GONE, list.getVisibility());

        assertEquals(0, list.getAdapter().getItemCount());

        // Switch back to all groups
        groupTabs.selectTab(groupTabs.getTabAt(0));

        assertEquals(View.GONE, helpSection.getVisibility());
        assertEquals(View.VISIBLE, noMatchingCardsText.getVisibility());
        assertEquals(View.GONE, list.getVisibility());

        assertEquals(0, list.getAdapter().getItemCount());

        mainActivity.mFilter = "";

        activityController.pause();
        activityController.resume();

        shadowOf(getMainLooper()).idle();

        assertEquals(View.GONE, helpSection.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(2, list.getAdapter().getItemCount());

        // Switch to Group one
        groupTabs.selectTab(groupTabs.getTabAt(1));

        activityController.pause();
        activityController.resume();

        shadowOf(getMainLooper()).idle();

        assertEquals(View.GONE, helpSection.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(1, list.getAdapter().getItemCount());

        // Switch back to all groups
        groupTabs.selectTab(groupTabs.getTabAt(0));

        assertEquals(View.GONE, helpSection.getVisibility());
        assertEquals(View.GONE, noMatchingCardsText.getVisibility());
        assertEquals(View.VISIBLE, list.getVisibility());

        assertEquals(2, list.getAdapter().getItemCount());

        database.close();
    }

    @Test
    public void testSearchQueryRestorationAfterNavigatingBack() {
        ActivityController activityController = Robolectric.buildActivity(MainActivity.class).create();

        MainActivity mainActivity = (MainActivity) activityController.get();
        activityController.start();
        activityController.resume();

        final Menu menu = shadowOf(Robolectric.setupActivity(MainActivity.class)).getOptionsMenu();
        MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        SearchView mSearchView = (SearchView) searchMenuItem.getActionView();


        SQLiteDatabase database = TestHelpers.getEmptyDb(mainActivity).getWritableDatabase();
        DBHelper.insertLoyaltyCard(database, "The First Store", "Initial note", null, null, new BigDecimal("0"), null, "cardId", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), Color.BLACK, 0, null,0);
        DBHelper.insertLoyaltyCard(database, "The Second Store", "Secondary note", null, null, new BigDecimal("0"), null, "cardId", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), Color.BLACK, 0, null,0);

        String finalQuery = "store";
        assert mSearchView != null;
        mSearchView.setQuery(finalQuery, false);

        activityController.pause();
        activityController.resume();

        // Simulation of what happens when users comes back after picking up card
        // We simulate expanding and setting the Query that we want to restore (in code it is from finalQuery String)
        searchMenuItem.expandActionView();

        mSearchView.setQuery(finalQuery, false);

        activityController.pause();
        activityController.resume();

        assertTrue(searchMenuItem.isActionViewExpanded());
        assertEquals("store", mSearchView.getQuery().toString());

        database.close();
    }
}