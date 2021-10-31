package protect.card_locker;

import android.app.SearchManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.recyclerview.widget.RecyclerView;

import protect.card_locker.databinding.MainActivityBinding;
import protect.card_locker.databinding.SortingOptionBinding;
import protect.card_locker.preferences.SettingsActivity;

public class MainActivity extends CatimaAppCompatActivity implements LoyaltyCardCursorAdapter.CardAdapterListener, GestureDetector.OnGestureListener
{
    private MainActivityBinding binding;
    private static final String TAG = "Catima";

    private final DBHelper mDB = new DBHelper(this);
    private LoyaltyCardCursorAdapter mAdapter;
    private ActionMode mCurrentActionMode;
    private Menu mMenu;
    private GestureDetector mGestureDetector;
    protected String mFilter = "";
    protected Object mGroup = null;
    protected DBHelper.LoyaltyCardOrder mOrder = DBHelper.LoyaltyCardOrder.Alpha;
    protected DBHelper.LoyaltyCardOrderDirection mOrderDirection = DBHelper.LoyaltyCardOrderDirection.Ascending;
    protected int selectedTab = 0;
    private RecyclerView mCardList;
    private View mHelpText;
    private View mNoMatchingCardsText;
    private View mNoGroupCardsText;

    private ActionMode.Callback mCurrentActionModeCallback = new ActionMode.Callback()
    {
        @Override
        public boolean onCreateActionMode(ActionMode inputMode, Menu inputMenu) {
            inputMode.getMenuInflater().inflate(R.menu.card_longclick_menu, inputMenu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode inputMode, Menu inputMenu)
        {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode inputMode, MenuItem inputItem) {
            if (inputItem.getItemId() == R.id.action_copy_to_clipboard) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

                String clipboardData;
                int cardCount = mAdapter.getSelectedItemCount();

                if (cardCount == 1) {
                    clipboardData = mAdapter.getSelectedItems().get(0).cardId;
                } else {
                    StringBuilder cardIds = new StringBuilder();

                    for (int i = 0; i < cardCount; i++) {
                        LoyaltyCard loyaltyCard = mAdapter.getSelectedItems().get(i);

                        cardIds.append(loyaltyCard.store + ": " + loyaltyCard.cardId);
                        if (i < (cardCount - 1)) {
                            cardIds.append("\n");
                        }
                    }

                    clipboardData = cardIds.toString();
                }

                ClipData clip = ClipData.newPlainText(getString(R.string.card_ids_copied), clipboardData);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, cardCount > 1 ? R.string.copy_to_clipboard_multiple_toast : R.string.copy_to_clipboard_toast, Toast.LENGTH_LONG).show();
                inputMode.finish();
                return true;
            } else if (inputItem.getItemId() == R.id.action_share) {
                final ImportURIHelper importURIHelper = new ImportURIHelper(MainActivity.this);
                try {
                    importURIHelper.startShareIntent(mAdapter.getSelectedItems());
                } catch (UnsupportedEncodingException e) {
                    Toast.makeText(MainActivity.this, R.string.failedGeneratingShareURL, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
                inputMode.finish();
                return true;
            } else if(inputItem.getItemId() == R.id.action_edit) {
                if (mAdapter.getSelectedItemCount() != 1) {
                    throw new IllegalArgumentException("Cannot edit more than 1 card at a time");
                }

                Intent intent = new Intent(getApplicationContext(), LoyaltyCardEditActivity.class);
                Bundle bundle = new Bundle();
                bundle.putInt(LoyaltyCardEditActivity.BUNDLE_ID, mAdapter.getSelectedItems().get(0).id);
                bundle.putBoolean(LoyaltyCardEditActivity.BUNDLE_UPDATE, true);
                intent.putExtras(bundle);
                startActivity(intent);
                inputMode.finish();
                return true;
            } else if(inputItem.getItemId() == R.id.action_delete) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                // The following may seem weird, but it is necessary to give translators enough flexibility.
                // For example, in Russian, Android's plural quantity "one" actually refers to "any number ending on 1 but not ending in 11".
                // So while in English the extra non-plural form seems unnecessary duplication, it is necessary to give translators enough flexibility.
                // In here, we use the plain string when meaning exactly 1, and otherwise use the plural forms
                if (mAdapter.getSelectedItemCount() == 1) {
                    builder.setTitle(R.string.deleteTitle);
                    builder.setMessage(R.string.deleteConfirmation);
                } else {
                    builder.setTitle(getResources().getQuantityString(R.plurals.deleteCardsTitle, mAdapter.getSelectedItemCount(), mAdapter.getSelectedItemCount()));
                    builder.setMessage(getResources().getQuantityString(R.plurals.deleteCardsConfirmation, mAdapter.getSelectedItemCount(), mAdapter.getSelectedItemCount()));
                }

                builder.setPositiveButton(R.string.confirm, (dialog, which) -> {
                    DBHelper db = new DBHelper(MainActivity.this);

                    for (LoyaltyCard loyaltyCard : mAdapter.getSelectedItems()) {
                        Log.e(TAG, "Deleting card: " + loyaltyCard.id);

                        db.deleteLoyaltyCard(loyaltyCard.id);

                        ShortcutHelper.removeShortcut(MainActivity.this, loyaltyCard.id);
                    }

                    TabLayout.Tab tab = binding.groups.getTabAt(selectedTab);
                    mGroup = tab != null ? tab.getTag() : null;

                    updateLoyaltyCardList();

                    dialog.dismiss();
                });
                builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                AlertDialog dialog = builder.create();
                dialog.show();

                return true;
            }

            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode inputMode)
        {
            mAdapter.clearSelections();
            mCurrentActionMode = null;
            mCardList.post(new Runnable()
            {
                @Override
                public void run()
                {
                    mAdapter.resetAnimationIndex();
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle inputSavedInstanceState)
    {
        super.onCreate(inputSavedInstanceState);
        binding = MainActivityBinding.inflate(getLayoutInflater());
        SplashScreen.installSplashScreen(this);
        setTitle(R.string.app_name);
        setContentView(binding.getRoot());
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);

        TabLayout groupsTabLayout = binding.groups;
        groupsTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTab = tab.getPosition();
                Log.d("onTabSelected","Tab Position "+tab.getPosition());
                mGroup = tab.getTag();
                updateLoyaltyCardList();
                // Store active tab in Shared Preference to restore next app launch
                SharedPreferences activeTabPref = getApplicationContext().getSharedPreferences(
                        getString(R.string.sharedpreference_active_tab),
                        Context.MODE_PRIVATE);
                SharedPreferences.Editor activeTabPrefEditor = activeTabPref.edit();
                activeTabPrefEditor.putInt(getString(R.string.sharedpreference_active_tab), tab.getPosition());
                activeTabPrefEditor.apply();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        mGestureDetector = new GestureDetector(this, this);

        View.OnTouchListener gestureTouchListener = (v, event) -> mGestureDetector.onTouchEvent(event);

        mHelpText = binding.contentMainLayout.helpText;
        mNoMatchingCardsText = binding.contentMainLayout.noMatchingCardsText;
        mNoGroupCardsText = binding.contentMainLayout.noGroupCardsText;
        mCardList = binding.contentMainLayout.list;

        mHelpText.setOnTouchListener(gestureTouchListener);
        mNoMatchingCardsText.setOnTouchListener(gestureTouchListener);
        mCardList.setOnTouchListener(gestureTouchListener);
        mNoGroupCardsText.setOnTouchListener(gestureTouchListener);

        mAdapter = new LoyaltyCardCursorAdapter(this, null, this);
        mCardList.setAdapter(mAdapter);
        registerForContextMenu(mCardList);

        mGroup = null;
        updateLoyaltyCardList();

        /*
         * This was added for Huawei, but Huawei is just too much of a fucking pain.
         * Just leaving this commented out if needed for the future idk
         * https://twitter.com/SylvieLorxu/status/1379437902741012483
         *

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
        }
         */
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if(mCurrentActionMode != null)
        {
            mAdapter.clearSelections();
            mCurrentActionMode.finish();
        }

        if (mMenu != null)
        {
            SearchView searchView = (SearchView) mMenu.findItem(R.id.action_search).getActionView();

            if (!searchView.isIconified())
            {
                mFilter = searchView.getQuery().toString();
            }
        }

        // Start of active tab logic
        TabLayout groupsTabLayout = binding.groups;
        updateTabGroups(groupsTabLayout);

        // Restore settings from Shared Preference
        SharedPreferences activeTabPref = getApplicationContext().getSharedPreferences(
                getString(R.string.sharedpreference_active_tab),
                Context.MODE_PRIVATE);
        selectedTab = activeTabPref.getInt(getString(R.string.sharedpreference_active_tab), 0);
        SharedPreferences sortPref = getApplicationContext().getSharedPreferences(
                getString(R.string.sharedpreference_sort),
                Context.MODE_PRIVATE);
        try {
            mOrder = DBHelper.LoyaltyCardOrder.valueOf(sortPref.getString(getString(R.string.sharedpreference_sort_order), null));
            mOrderDirection = DBHelper.LoyaltyCardOrderDirection.valueOf(sortPref.getString(getString(R.string.sharedpreference_sort_direction), null));
        } catch (IllegalArgumentException | NullPointerException ignored) {}

        mGroup = null;

        if (groupsTabLayout.getTabCount() != 0) {
            TabLayout.Tab tab = groupsTabLayout.getTabAt(selectedTab);
            if (tab == null) {
                tab = groupsTabLayout.getTabAt(0);
            }

            groupsTabLayout.selectTab(tab);
            assert tab != null;
            mGroup = tab.getTag();
        }
        updateLoyaltyCardList();
        // End of active tab logic

        FloatingActionButton addButton = binding.fabAdd;
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), ScanActivity.class);
            Bundle bundle = new Bundle();
            if (selectedTab != 0) {
                bundle.putString(LoyaltyCardEditActivity.BUNDLE_ADDGROUP, groupsTabLayout.getTabAt(selectedTab).getText().toString());
            }
            intent.putExtras(bundle);
            startActivityForResult(intent, Utils.BARCODE_SCAN);
        });
        addButton.bringToFront();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == Utils.MAIN_REQUEST) {
            // We're coming back from another view so clear the search
            // We only do this now to prevent a flash of all entries right after picking one
            mFilter = "";
            if (mMenu != null)
            {
                MenuItem searchItem = mMenu.findItem(R.id.action_search);
                searchItem.collapseActionView();
            }
            updateLoyaltyCardList();

            return;
        }

        BarcodeValues barcodeValues = Utils.parseSetBarcodeActivityResult(requestCode, resultCode, intent, this);

        if(!barcodeValues.isEmpty()) {
            Intent newIntent = new Intent(getApplicationContext(), LoyaltyCardEditActivity.class);
            Bundle newBundle = new Bundle();
            newBundle.putString(LoyaltyCardEditActivity.BUNDLE_BARCODETYPE, barcodeValues.format());
            newBundle.putString(LoyaltyCardEditActivity.BUNDLE_CARDID, barcodeValues.content());
            Bundle inputBundle = intent.getExtras();
            if (inputBundle != null && inputBundle.getString(LoyaltyCardEditActivity.BUNDLE_ADDGROUP) != null) {
                newBundle.putString(LoyaltyCardEditActivity.BUNDLE_ADDGROUP, inputBundle.getString(LoyaltyCardEditActivity.BUNDLE_ADDGROUP));
            }
            newIntent.putExtras(newBundle);
            startActivity(newIntent);
        }
    }

    @Override
    public void onBackPressed()
    {
        if (mMenu != null) {
            SearchView searchView = (SearchView) mMenu.findItem(R.id.action_search).getActionView();

            if (!searchView.isIconified()) {
                searchView.setIconified(true);
                return;
            }
        }

        super.onBackPressed();
    }

    private void updateLoyaltyCardList() {
        Group group = null;
        if (mGroup != null) {
            group = (Group) mGroup;
        }

        mAdapter.swapCursor(mDB.getLoyaltyCardCursor(mFilter, group, mOrder, mOrderDirection));

        if(mDB.getLoyaltyCardCount() > 0)
        {
            // We want the cardList to be visible regardless of the filtered match count
            // to ensure that the noMatchingCardsText doesn't end up being shown below
            // the keyboard
            mHelpText.setVisibility(View.GONE);
            mNoGroupCardsText.setVisibility(View.GONE);
            if(mAdapter.getItemCount() > 0)
            {
                mCardList.setVisibility(View.VISIBLE);
                mNoMatchingCardsText.setVisibility(View.GONE);
            }
            else
            {
                mCardList.setVisibility(View.GONE);
                if (!mFilter.isEmpty()) {
                    // Actual Empty Search Result
                    mNoMatchingCardsText.setVisibility(View.VISIBLE);
                    mNoGroupCardsText.setVisibility(View.GONE);
                } else {
                    // Group Tab with no Group Cards
                    mNoMatchingCardsText.setVisibility(View.GONE);
                    mNoGroupCardsText.setVisibility(View.VISIBLE);
                }
            }
        }
        else
        {
            mCardList.setVisibility(View.GONE);
            mHelpText.setVisibility(View.VISIBLE);
            mNoMatchingCardsText.setVisibility(View.GONE);
            mNoGroupCardsText.setVisibility(View.GONE);
        }

        if (mCurrentActionMode != null) {
            mCurrentActionMode.finish();
        }
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

    @Override
    public boolean onCreateOptionsMenu(Menu inputMenu)
    {
        this.mMenu = inputMenu;

        getMenuInflater().inflate(R.menu.main_menu, inputMenu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null)
        {
            SearchView searchView = (SearchView) inputMenu.findItem(R.id.action_search).getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setSubmitButtonEnabled(false);

            searchView.setOnCloseListener(() -> {
                invalidateOptionsMenu();
                return false;
            });

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
            {
                @Override
                public boolean onQueryTextSubmit(String query)
                {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText)
                {
                    mFilter = newText;

                    TabLayout groupsTabLayout = binding.groups;
                    TabLayout.Tab currentTab = groupsTabLayout.getTabAt(groupsTabLayout.getSelectedTabPosition());
                    mGroup = currentTab != null ? currentTab.getTag() : null;

                    updateLoyaltyCardList();

                    return true;
                }
            });
        }
        return super.onCreateOptionsMenu(inputMenu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem inputItem)
    {
        int id = inputItem.getItemId();

        if (id == R.id.action_sort)
        {
            TabLayout.Tab tab = binding.groups.getTabAt(selectedTab);
            AtomicInteger currentIndex = new AtomicInteger();
            List<DBHelper.LoyaltyCardOrder> loyaltyCardOrders = Arrays.asList(DBHelper.LoyaltyCardOrder.values());
            for (int i = 0; i < loyaltyCardOrders.size(); i++) {
                if (mOrder == loyaltyCardOrders.get(i)) {
                    currentIndex.set(i);
                    break;
                }
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(R.string.sort_by);

            final SortingOptionBinding customLayout = SortingOptionBinding.inflate(getLayoutInflater());
            builder.setView(customLayout.getRoot());

            CheckBox ch = (CheckBox) customLayout.checkBoxReverse;
            ch.setChecked(mOrderDirection == DBHelper.LoyaltyCardOrderDirection.Descending);

            builder.setSingleChoiceItems(R.array.sort_types_array, currentIndex.get(), (dialog, which) -> currentIndex.set(which));

            builder.setPositiveButton(R.string.sort, (dialog, which) -> {
                if(ch.isChecked()) {
                    setSort(loyaltyCardOrders.get(currentIndex.get()), DBHelper.LoyaltyCardOrderDirection.Descending);
                }
                else {
                    setSort(loyaltyCardOrders.get(currentIndex.get()), DBHelper.LoyaltyCardOrderDirection.Ascending);
                }
                dialog.dismiss();
            });

            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

            AlertDialog dialog = builder.create();
            dialog.show();

            return true;
        }

        if (id == R.id.action_manage_groups)
        {
            Intent i = new Intent(getApplicationContext(), ManageGroupsActivity.class);
            startActivityForResult(i, Utils.MAIN_REQUEST);
            return true;
        }

        if (id == R.id.action_import_export)
        {
            Intent i = new Intent(getApplicationContext(), ImportExportActivity.class);
            startActivityForResult(i, Utils.MAIN_REQUEST);
            return true;
        }

        if (id == R.id.action_settings)
        {
            Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivityForResult(i, Utils.MAIN_REQUEST);
            return true;
        }

        if (id == R.id.action_about)
        {
            Intent i = new Intent(getApplicationContext(), AboutActivity.class);
            startActivityForResult(i, Utils.MAIN_REQUEST);
            return true;
        }

        return super.onOptionsItemSelected(inputItem);
    }

    private void setSort(DBHelper.LoyaltyCardOrder order, DBHelper.LoyaltyCardOrderDirection direction) {
        // Update values
        mOrder = order;
        mOrderDirection = direction;

        // Store in Shared Preference to restore next app launch
        SharedPreferences sortPref = getApplicationContext().getSharedPreferences(
                getString(R.string.sharedpreference_sort),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor sortPrefEditor = sortPref.edit();
        sortPrefEditor.putString(getString(R.string.sharedpreference_sort_order), order.name());
        sortPrefEditor.putString(getString(R.string.sharedpreference_sort_direction), direction.name());
        sortPrefEditor.apply();

        // Update card list
        updateLoyaltyCardList();
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
    public boolean dispatchTouchEvent(MotionEvent ev) {
        mGestureDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        Log.d(TAG, "On fling");

        // Don't swipe if we have too much vertical movement
        if (Math.abs(velocityY) > (0.75 * Math.abs(velocityX))) {
            return false;
        }

        TabLayout groupsTabLayout = binding.groups;
        if (groupsTabLayout.getTabCount() < 2) {
            return false;
        }

        Integer currentTab = groupsTabLayout.getSelectedTabPosition();
        Log.d("onFling","Current Tab "+currentTab);
        // Swipe right
        if (velocityX < -150) {
            Log.d("onFling","Right Swipe detected "+velocityX);
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
            Log.d("onFling","Left Swipe detected "+velocityX);
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

    @Override
    public void onRowLongClicked(int inputPosition)
    {
        enableActionMode(inputPosition);
    }

    private void enableActionMode(int inputPosition)
    {
        if (mCurrentActionMode == null)
        {
            mCurrentActionMode = startSupportActionMode(mCurrentActionModeCallback);
        }
        toggleSelection(inputPosition);
    }

    private void toggleSelection(int inputPosition)
    {
        mAdapter.toggleSelection(inputPosition);
        int count = mAdapter.getSelectedItemCount();

        if (count == 0) {
            mCurrentActionMode.finish();
        } else {
            mCurrentActionMode.setTitle(getResources().getQuantityString(R.plurals.selectedCardCount, count, count));

            MenuItem editItem = mCurrentActionMode.getMenu().findItem(R.id.action_edit);
            if (count == 1) {
                editItem.setVisible(true);
                editItem.setEnabled(true);
            } else {
                editItem.setVisible(false);
                editItem.setEnabled(false);
            }

            mCurrentActionMode.invalidate();
        }
    }


    @Override
    public void onRowClicked(int inputPosition)
    {
        if (mAdapter.getSelectedItemCount() > 0)
        {
            enableActionMode(inputPosition);
        }
        else
        {
            Cursor selected = mAdapter.getCursor();
            selected.moveToPosition(inputPosition);
            // FIXME
            //
            // There is a really nasty edge case that can happen when someone taps a card but right
            // after it swipes (very small window, hard to reproduce). The cursor gets replaced and
            // may not have a card at the ID number that is returned from onRowClicked.
            //
            // The proper fix, obviously, would involve makes sure an onFling can't happen while a
            // click is being processed. Sadly, I have not yet found a way to make that possible.
            LoyaltyCard loyaltyCard;
            try {
                loyaltyCard = LoyaltyCard.toLoyaltyCard(selected);
            } catch (CursorIndexOutOfBoundsException e) {
                Log.w(TAG, "Prevented crash from tap + swipe on ID " + inputPosition + ": " + e);
                return;
            }

            Intent i = new Intent(this, LoyaltyCardViewActivity.class);
            i.setAction("");
            final Bundle b = new Bundle();
            b.putInt("id", loyaltyCard.id);
            i.putExtras(b);

            ShortcutHelper.updateShortcuts(MainActivity.this, loyaltyCard);

            startActivityForResult(i, Utils.MAIN_REQUEST);
        }
    }
}
