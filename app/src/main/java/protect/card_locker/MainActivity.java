package protect.card_locker;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import java.util.List;

import protect.card_locker.preferences.SettingsActivity;

public class MainActivity extends AppCompatActivity implements GestureDetector.OnGestureListener
{
    private static final String TAG = "Catima";

    private Menu menu;
    private GestureDetector gestureDetector;
    protected String filter = "";
    protected int selectedTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        updateLoyaltyCardList(filter, null);

        TabLayout groupsTabLayout = findViewById(R.id.groups);
        groupsTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTab = tab.getPosition();
                updateLoyaltyCardList(filter, tab.getTag());

                // Store active tab in Shared Preference to restore next app launch
                SharedPreferences activeTabPref = getApplicationContext().getSharedPreferences(
                        getString(R.string.sharedpreference_active_tab),
                        Context.MODE_PRIVATE);
                SharedPreferences.Editor activeTabPrefEditor = activeTabPref.edit();
                activeTabPrefEditor.putInt(getString(R.string.sharedpreference_active_tab), selectedTab);
                activeTabPrefEditor.apply();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        gestureDetector = new GestureDetector(this, this);

        View.OnTouchListener gestureTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View v, final MotionEvent event){
                return gestureDetector.onTouchEvent(event);
            }
        };

        final View helpText = findViewById(R.id.helpText);
        final View noMatchingCardsText = findViewById(R.id.noMatchingCardsText);
        final View list = findViewById(R.id.list);

        helpText.setOnTouchListener(gestureTouchListener);
        noMatchingCardsText.setOnTouchListener(gestureTouchListener);
        list.setOnTouchListener(gestureTouchListener);

        // Show privacy policy on first run
        SharedPreferences privacyPolicyShownPref = getApplicationContext().getSharedPreferences(
                getString(R.string.sharedpreference_privacy_policy_shown),
                Context.MODE_PRIVATE);

        if (privacyPolicyShownPref.getInt(getString(R.string.sharedpreference_privacy_policy_shown), 0) == 0) {
            SharedPreferences.Editor privacyPolicyShownPrefEditor = privacyPolicyShownPref.edit();
            privacyPolicyShownPrefEditor.putInt(getString(R.string.sharedpreference_privacy_policy_shown), 1);
            privacyPolicyShownPrefEditor.apply();

            new AlertDialog.Builder(this)
                    .setTitle(R.string.privacy_policy)
                    .setMessage(R.string.privacy_policy_popup_text)
                    .setPositiveButton(R.string.accept, null)
                    .setNegativeButton(R.string.privacy_policy, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            openPrivacyPolicy();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show();
        };
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (menu != null)
        {
            SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

            if (!searchView.isIconified()) {
                filter = searchView.getQuery().toString();
            }
        }

        // Start of active tab logic
        TabLayout groupsTabLayout = findViewById(R.id.groups);
        updateTabGroups(groupsTabLayout);

        // Restore active tab from Shared Preference
        SharedPreferences activeTabPref = getApplicationContext().getSharedPreferences(
                getString(R.string.sharedpreference_active_tab),
                Context.MODE_PRIVATE);
        selectedTab = activeTabPref.getInt(getString(R.string.sharedpreference_active_tab), 0);

        Object group = null;

        if (groupsTabLayout.getTabCount() != 0) {
            TabLayout.Tab tab = groupsTabLayout.getTabAt(selectedTab);
            if (tab == null) {
                tab = groupsTabLayout.getTabAt(0);
            }

            groupsTabLayout.selectTab(tab);
            assert tab != null;
            group = tab.getTag();
        }
        updateLoyaltyCardList(filter, group);
        // End of active tab logic

        FloatingActionButton addButton = findViewById(R.id.fabAdd);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), ScanActivity.class);
                startActivityForResult(i, Utils.BARCODE_SCAN);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == Utils.MAIN_REQUEST) {
            // We're coming back from another view so clear the search
            // We only do this now to prevent a flash of all entries right after picking one
            filter = "";
            if (menu != null)
            {
                MenuItem searchItem = menu.findItem(R.id.action_search);
                searchItem.collapseActionView();
            }

            // In case the theme changed
            recreate();

            return;
        }

        BarcodeValues barcodeValues = Utils.parseSetBarcodeActivityResult(requestCode, resultCode, intent);

        if(!barcodeValues.isEmpty()) {
            Intent newIntent = new Intent(getApplicationContext(), LoyaltyCardEditActivity.class);
            Bundle newBundle = new Bundle();
            newBundle.putString("barcodeType", barcodeValues.format());
            newBundle.putString("cardId", barcodeValues.content());
            newIntent.putExtras(newBundle);
            startActivity(newIntent);
        }
    }

    @Override
    public void onBackPressed() {
        if (menu == null)
        {
            super.onBackPressed();
            return;
        }

        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        if (!searchView.isIconified()) {
            searchView.setIconified(true);
        } else {
            TabLayout groupsTabLayout = findViewById(R.id.groups);

            if (groupsTabLayout.getVisibility() == View.VISIBLE && selectedTab != 0) {
                selectedTab = 0;
                groupsTabLayout.selectTab(groupsTabLayout.getTabAt(0));
            } else {
                super.onBackPressed();
            }
        }
    }

    private void updateLoyaltyCardList(String filterText, Object tag)
    {
        Group group = null;
        if (tag != null) {
            group = (Group) tag;
        }

        final ListView cardList = findViewById(R.id.list);
        final TextView helpText = findViewById(R.id.helpText);
        final TextView noMatchingCardsText = findViewById(R.id.noMatchingCardsText);
        final DBHelper db = new DBHelper(this);

        Cursor cardCursor = db.getLoyaltyCardCursor(filterText, group);

        if(db.getLoyaltyCardCount() > 0)
        {
            // We want the cardList to be visible regardless of the filtered match count
            // to ensure that the noMatchingCardsText doesn't end up being shown below
            // the keyboard
            cardList.setVisibility(View.VISIBLE);
            helpText.setVisibility(View.GONE);
            if(cardCursor.getCount() > 0)
            {
                noMatchingCardsText.setVisibility(View.GONE);
            }
            else
            {
                noMatchingCardsText.setVisibility(View.VISIBLE);
            }
        }
        else
        {
            cardList.setVisibility(View.GONE);
            helpText.setVisibility(View.VISIBLE);
            noMatchingCardsText.setVisibility(View.GONE);
        }

        final LoyaltyCardCursorAdapter adapter = new LoyaltyCardCursorAdapter(this, cardCursor);
        cardList.setAdapter(adapter);

        registerForContextMenu(cardList);

        cardList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Cursor selected = (Cursor) parent.getItemAtPosition(position);
                LoyaltyCard loyaltyCard = LoyaltyCard.toLoyaltyCard(selected);

                Intent i = new Intent(view.getContext(), LoyaltyCardViewActivity.class);
                i.setAction("");
                final Bundle b = new Bundle();
                b.putInt("id", loyaltyCard.id);
                i.putExtras(b);

                ShortcutHelper.updateShortcuts(MainActivity.this, loyaltyCard, i);

                startActivityForResult(i, Utils.MAIN_REQUEST);
            }
        });
    }

    public void updateTabGroups(TabLayout groupsTabLayout)
    {
        final DBHelper db = new DBHelper(this);

        List<Group> newGroups = db.getGroups();

        if (newGroups.size() == 0) {
            groupsTabLayout.removeAllTabs();
            groupsTabLayout.setVisibility(View.GONE);
            return;
        }

        groupsTabLayout.removeAllTabs();

        TabLayout.Tab allTab = groupsTabLayout.newTab();
        allTab.setText(R.string.all);
        allTab.setTag(null);
        groupsTabLayout.addTab(allTab, false);

        for (Group group : newGroups) {
            TabLayout.Tab tab = groupsTabLayout.newTab();
            tab.setText(group._id);
            tab.setTag(group);
            groupsTabLayout.addTab(tab, false);
        }

        groupsTabLayout.setVisibility(View.VISIBLE);
    }

    private void openPrivacyPolicy() {
        Intent browserIntent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://thelastproject.github.io/Catima/privacy-policy")
        );
        startActivity(browserIntent);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId()==R.id.list)
        {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.card_longclick_menu, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        ListView listView = findViewById(R.id.list);

        Cursor cardCursor = (Cursor)listView.getItemAtPosition(info.position);
        LoyaltyCard card = LoyaltyCard.toLoyaltyCard(cardCursor);

        if(item.getItemId() == R.id.action_clipboard)
        {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(card.store, card.cardId);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(this, R.string.copy_to_clipboard_toast, Toast.LENGTH_LONG).show();
            return true;
        }
        else if(item.getItemId() == R.id.action_share)
        {
            final ImportURIHelper importURIHelper = new ImportURIHelper(this);
            importURIHelper.startShareIntent(card);
            return true;
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        this.menu = menu;

        getMenuInflater().inflate(R.menu.main_menu, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null) {
            SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setSubmitButtonEnabled(false);

            searchView.setOnCloseListener(new SearchView.OnCloseListener() {
                @Override
                public boolean onClose() {
                    invalidateOptionsMenu();
                    return false;
                }
            });

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    filter = newText;

                    TabLayout groupsTabLayout = findViewById(R.id.groups);
                    TabLayout.Tab currentTab = groupsTabLayout.getTabAt(groupsTabLayout.getSelectedTabPosition());

                    updateLoyaltyCardList(
                        newText,
                        currentTab != null ? currentTab.getTag() : null
                    );

                    return true;
                }
            });
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.action_manage_groups)
        {
            Intent i = new Intent(getApplicationContext(), ManageGroupsActivity.class);
            startActivityForResult(i, Utils.MAIN_REQUEST);
            return true;
        }

        if(id == R.id.action_import_export)
        {
            Intent i = new Intent(getApplicationContext(), ImportExportActivity.class);
            startActivityForResult(i, Utils.MAIN_REQUEST);
            return true;
        }

        if(id == R.id.action_settings)
        {
            Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivityForResult(i, Utils.MAIN_REQUEST);
            return true;
        }

        if(id == R.id.action_privacy_policy)
        {
            openPrivacyPolicy();
            return true;
        }

        if(id == R.id.action_about)
        {
            Intent i = new Intent(getApplicationContext(), AboutActivity.class);
            startActivityForResult(i, Utils.MAIN_REQUEST);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected static boolean isDarkModeEnabled(Context inputContext)
    {
        Configuration config = inputContext.getResources().getConfiguration();
        int currentNightMode = config.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return (currentNightMode == Configuration.UI_MODE_NIGHT_YES);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        Log.d(TAG, "On fling");

        // Don't swipe if we have too much vertical movement
        if (Math.abs(velocityY) > (0.75 * Math.abs(velocityX))) {
            return false;
        }

        TabLayout groupsTabLayout = findViewById(R.id.groups);
        if (groupsTabLayout.getTabCount() < 2) {
            return false;
        }

        Integer currentTab = groupsTabLayout.getSelectedTabPosition();

        // Swipe right
        if (velocityX < -150) {
            Integer nextTab = currentTab + 1;

            if (nextTab == groupsTabLayout.getTabCount()) {
                groupsTabLayout.selectTab(groupsTabLayout.getTabAt(0));
            } else {
                groupsTabLayout.selectTab(groupsTabLayout.getTabAt(nextTab));
            }

            return true;
        }

        // Swipe left
        if (velocityX > 150) {
            Integer nextTab = currentTab - 1;

            if (nextTab < 0) {
                groupsTabLayout.selectTab(groupsTabLayout.getTabAt(groupsTabLayout.getTabCount() - 1));
            } else {
                groupsTabLayout.selectTab(groupsTabLayout.getTabAt(nextTab));
            }

            return true;
        }

        return false;
    }
}