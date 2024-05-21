package protect.card_locker;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.core.splashscreen.SplashScreen;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import protect.card_locker.databinding.ContentMainBinding;
import protect.card_locker.databinding.MainActivityBinding;
import protect.card_locker.databinding.SortingOptionBinding;
import protect.card_locker.importexport.DataFormat;
import protect.card_locker.importexport.ImportExportWorker;
import protect.card_locker.preferences.SettingsActivity;

public class MainActivity extends CatimaAppCompatActivity implements LoyaltyCardCursorAdapter.CardAdapterListener {
    private MainActivityBinding binding;
    private ContentMainBinding contentMainBinding;
    private static final String TAG = "Catima";
    public static final String RESTART_ACTIVITY_INTENT = "restart_activity_intent";

    private static final int MEDIUM_SCALE_FACTOR_DIP = 460;

    private SQLiteDatabase mDatabase;
    private LoyaltyCardCursorAdapter mAdapter;
    private ActionMode mCurrentActionMode;
    private SearchView mSearchView;
    private int mLoyaltyCardCount = 0;
    protected String mFilter = "";
    protected Object mGroup = null;
    protected DBHelper.LoyaltyCardOrder mOrder = DBHelper.LoyaltyCardOrder.Alpha;
    protected DBHelper.LoyaltyCardOrderDirection mOrderDirection = DBHelper.LoyaltyCardOrderDirection.Ascending;
    protected int selectedTab = 0;
    private RecyclerView mCardList;
    private View mHelpSection;
    private View mNoMatchingCardsText;
    private View mNoGroupCardsText;
    private TabLayout groupsTabLayout;

    private Runnable mUpdateLoyaltyCardListRunnable;

    private ActivityResultLauncher<Intent> mBarcodeScannerLauncher;
    private ActivityResultLauncher<Intent> mSettingsLauncher;
    private ActivityResultLauncher<Intent> mImportExportLauncher;

    private ActionMode.Callback mCurrentActionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode inputMode, Menu inputMenu) {
            inputMode.getMenuInflater().inflate(R.menu.card_longclick_menu, inputMenu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode inputMode, Menu inputMenu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode inputMode, MenuItem inputItem) {
            if (inputItem.getItemId() == R.id.action_share) {
                final ImportURIHelper importURIHelper = new ImportURIHelper(MainActivity.this);
                try {
                    importURIHelper.startShareIntent(mAdapter.getSelectedItems());
                } catch (UnsupportedEncodingException e) {
                    Toast.makeText(MainActivity.this, R.string.failedGeneratingShareURL, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
                inputMode.finish();
                return true;
            } else if (inputItem.getItemId() == R.id.action_edit) {
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
            } else if (inputItem.getItemId() == R.id.action_delete) {
                AlertDialog.Builder builder = new MaterialAlertDialogBuilder(MainActivity.this);
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
                    for (LoyaltyCard loyaltyCard : mAdapter.getSelectedItems()) {
                        Log.d(TAG, "Deleting card: " + loyaltyCard.id);

                        DBHelper.deleteLoyaltyCard(mDatabase, MainActivity.this, loyaltyCard.id);

                        ShortcutHelper.removeShortcut(MainActivity.this, loyaltyCard.id);
                    }

                    TabLayout.Tab tab = groupsTabLayout.getTabAt(selectedTab);
                    mGroup = tab != null ? tab.getTag() : null;

                    updateLoyaltyCardList(true);

                    dialog.dismiss();
                });
                builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                AlertDialog dialog = builder.create();
                dialog.show();

                return true;
            } else if (inputItem.getItemId() == R.id.action_archive) {
                for (LoyaltyCard loyaltyCard : mAdapter.getSelectedItems()) {
                    Log.d(TAG, "Archiving card: " + loyaltyCard.id);
                    DBHelper.updateLoyaltyCardArchiveStatus(mDatabase, loyaltyCard.id, 1);
                    ShortcutHelper.removeShortcut(MainActivity.this, loyaltyCard.id);
                    updateLoyaltyCardList(false);
                    inputMode.finish();
                    invalidateOptionsMenu();
                }
                return true;
            } else if (inputItem.getItemId() == R.id.action_unarchive) {
                for (LoyaltyCard loyaltyCard : mAdapter.getSelectedItems()) {
                    Log.d(TAG, "Unarchiving card: " + loyaltyCard.id);
                    DBHelper.updateLoyaltyCardArchiveStatus(mDatabase, loyaltyCard.id, 0);
                    updateLoyaltyCardList(false);
                    inputMode.finish();
                    invalidateOptionsMenu();
                }
                return true;
            } else if (inputItem.getItemId() == R.id.action_star) {
                for (LoyaltyCard loyaltyCard : mAdapter.getSelectedItems()) {
                    Log.d(TAG, "Starring card: " + loyaltyCard.id);
                    DBHelper.updateLoyaltyCardStarStatus(mDatabase, loyaltyCard.id, 1);
                    updateLoyaltyCardList(false);
                    inputMode.finish();
                }
                return true;
            } else if (inputItem.getItemId() == R.id.action_unstar) {
                for (LoyaltyCard loyaltyCard : mAdapter.getSelectedItems()) {
                    Log.d(TAG, "Unstarring card: " + loyaltyCard.id);
                    DBHelper.updateLoyaltyCardStarStatus(mDatabase, loyaltyCard.id, 0);
                    updateLoyaltyCardList(false);
                    inputMode.finish();
                }
                return true;
            }

            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode inputMode) {
            mAdapter.clearSelections();
            mCurrentActionMode = null;
        }
    };

    @Override
    protected void onCreate(Bundle inputSavedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(inputSavedInstanceState);

        // We should extract the share intent after we called the super.onCreate as it may need to spawn a dialog window and the app needs to be initialized to not crash
        extractIntentFields(getIntent());

        binding = MainActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        groupsTabLayout = binding.groups;
        contentMainBinding = ContentMainBinding.bind(binding.include.getRoot());

        mDatabase = new DBHelper(this).getWritableDatabase();

        mUpdateLoyaltyCardListRunnable = () -> {
            updateLoyaltyCardList(false);
        };

        groupsTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTab = tab.getPosition();
                Log.d("onTabSelected", "Tab Position " + tab.getPosition());
                mGroup = tab.getTag();
                updateLoyaltyCardList(false);
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

        mHelpSection = contentMainBinding.helpSection;
        mNoMatchingCardsText = contentMainBinding.noMatchingCardsText;
        mNoGroupCardsText = contentMainBinding.noGroupCardsText;
        mCardList = contentMainBinding.list;

        mAdapter = new LoyaltyCardCursorAdapter(this, null, this, mUpdateLoyaltyCardListRunnable);
        mCardList.setAdapter(mAdapter);
        registerForContextMenu(mCardList);

        mGroup = null;
        updateLoyaltyCardList(true);

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

        mBarcodeScannerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            // Exit early if the user cancelled the scan (pressed back/home)
            if (result.getResultCode() != RESULT_OK) {
                return;
            }

            Intent intent = result.getData();
            List<BarcodeValues> barcodeValuesList = Utils.parseSetBarcodeActivityResult(Utils.BARCODE_SCAN, result.getResultCode(), intent, this);

            Bundle inputBundle = intent.getExtras();
            String group = inputBundle != null ? inputBundle.getString(LoyaltyCardEditActivity.BUNDLE_ADDGROUP) : null;
            processBarcodeValuesList(barcodeValuesList, group, false);
        });

        mSettingsLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent intent = result.getData();
                if (intent != null && intent.getBooleanExtra(RESTART_ACTIVITY_INTENT, false)) {
                    recreate();
                }
            }
        });

        mImportExportLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            // User didn't ask for import or export
            if (result.getResultCode() != RESULT_OK) {
                return;
            }

            // Watch for active imports/exports
            new Thread(() -> {
                WorkManager workManager = WorkManager.getInstance(MainActivity.this);

                Snackbar importRunning = Snackbar.make(binding.getRoot(), R.string.importing, Snackbar.LENGTH_INDEFINITE);

                while (true) {
                    try {
                        List<WorkInfo> activeImports = workManager.getWorkInfosForUniqueWork(ImportExportWorker.ACTION_IMPORT).get();

                        // We should only have one import running at a time, so it should be safe to always grab the latest
                        WorkInfo activeImport = activeImports.get(activeImports.size() - 1);
                        WorkInfo.State importState = activeImport.getState();

                        if (importState == WorkInfo.State.RUNNING || importState == WorkInfo.State.ENQUEUED || importState == WorkInfo.State.BLOCKED) {
                            importRunning.show();
                        } else if (importState == WorkInfo.State.SUCCEEDED) {
                            importRunning.dismiss();
                            runOnUiThread(() -> {
                                Toast.makeText(getApplicationContext(), getString(R.string.importSuccessful), Toast.LENGTH_LONG).show();
                                updateLoyaltyCardList(true);
                            });

                            break;
                        } else {
                            importRunning.dismiss();

                            Data outputData = activeImport.getOutputData();

                            // FIXME: This dialog will asynchronously be accepted or declined and we don't know the status of it so we can't show the import state
                            // We want to get back into this function
                            // A cheap fix would be to keep looping but if the user dismissed the dialog that could mean we're looping forever...
                            if (Objects.equals(outputData.getString(ImportExportWorker.OUTPUT_ERROR_REASON), ImportExportWorker.ERROR_PASSWORD_REQUIRED)) {
                                runOnUiThread(() -> ImportExportActivity.retryWithPassword(
                                        MainActivity.this,
                                        DataFormat.valueOf(outputData.getString(ImportExportWorker.INPUT_FORMAT)),
                                        Uri.parse(outputData.getString(ImportExportWorker.INPUT_URI))
                                ));
                            } else {
                                runOnUiThread(() -> {
                                    Toast.makeText(getApplicationContext(), getString(R.string.importFailed), Toast.LENGTH_LONG).show();
                                    Toast.makeText(getApplicationContext(), activeImport.getOutputData().getString(ImportExportWorker.OUTPUT_ERROR_REASON), Toast.LENGTH_LONG).show();
                                    Toast.makeText(getApplicationContext(), activeImport.getOutputData().getString(ImportExportWorker.OUTPUT_ERROR_DETAILS), Toast.LENGTH_LONG).show();
                                });
                            }

                            break;
                        }
                    } catch (ExecutionException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mSearchView != null && !mSearchView.isIconified()) {
                    mSearchView.setIconified(true);
                } else {
                    finish();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mCurrentActionMode != null) {
            mAdapter.clearSelections();
            mCurrentActionMode.finish();
        }

        if (mSearchView != null && !mSearchView.isIconified()) {
            mFilter = mSearchView.getQuery().toString();
        }

        // Start of active tab logic
        updateTabGroups(groupsTabLayout);

        // Restore selected tab from Shared Preference
        SharedPreferences activeTabPref = getApplicationContext().getSharedPreferences(
                getString(R.string.sharedpreference_active_tab),
                Context.MODE_PRIVATE);
        selectedTab = activeTabPref.getInt(getString(R.string.sharedpreference_active_tab), 0);

        // Restore sort preferences from Shared Preferences
        // If one of the sorting prefererences has never been set or is set to an invalid value,
        // stick to the defaults.
        SharedPreferences sortPref = getApplicationContext().getSharedPreferences(
                getString(R.string.sharedpreference_sort),
                Context.MODE_PRIVATE);

        String orderString = sortPref.getString(getString(R.string.sharedpreference_sort_order), null);
        String orderDirectionString = sortPref.getString(getString(R.string.sharedpreference_sort_direction), null);

        if (orderString != null && orderDirectionString != null) {
            try {
                mOrder = DBHelper.LoyaltyCardOrder.valueOf(orderString);
                mOrderDirection = DBHelper.LoyaltyCardOrderDirection.valueOf(orderDirectionString);
            } catch (IllegalArgumentException ignored) {
            }
        }

        mGroup = null;

        if (groupsTabLayout.getTabCount() != 0) {
            TabLayout.Tab tab = groupsTabLayout.getTabAt(selectedTab);
            if (tab == null) {
                tab = groupsTabLayout.getTabAt(0);
            }

            groupsTabLayout.selectTab(tab);
            assert tab != null;
            mGroup = tab.getTag();
        } else {
            scaleScreen();
        }

        updateLoyaltyCardList(true);
        // End of active tab logic

        FloatingActionButton addButton = binding.fabAdd;

        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), ScanActivity.class);
            Bundle bundle = new Bundle();
            if (selectedTab != 0) {
                bundle.putString(LoyaltyCardEditActivity.BUNDLE_ADDGROUP, groupsTabLayout.getTabAt(selectedTab).getText().toString());
            }
            intent.putExtras(bundle);
            mBarcodeScannerLauncher.launch(intent);
        });
        addButton.bringToFront();
    }

    private void displayCardSetupOptions(Menu menu, boolean shouldShow) {
        for (int id : new int[]{R.id.action_search, R.id.action_display_options, R.id.action_sort}) {
            menu.findItem(id).setVisible(shouldShow);
        }
    }

    private void updateLoyaltyCardCount() {
        mLoyaltyCardCount = DBHelper.getLoyaltyCardCount(mDatabase);
    }

    private void updateLoyaltyCardList(boolean updateCount) {
        Group group = null;
        if (mGroup != null) {
            group = (Group) mGroup;
        }

        mAdapter.swapCursor(DBHelper.getLoyaltyCardCursor(mDatabase, mFilter, group, mOrder, mOrderDirection, mAdapter.showingArchivedCards() ? DBHelper.LoyaltyCardArchiveFilter.All : DBHelper.LoyaltyCardArchiveFilter.Unarchived));

        if (updateCount) {
            updateLoyaltyCardCount();
            // Update menu icons if necessary
            invalidateOptionsMenu();
        }

        if (mLoyaltyCardCount > 0) {
            // We want the cardList to be visible regardless of the filtered match count
            // to ensure that the noMatchingCardsText doesn't end up being shown below
            // the keyboard
            mHelpSection.setVisibility(View.GONE);
            mNoGroupCardsText.setVisibility(View.GONE);

            if (mAdapter.getItemCount() > 0) {
                mCardList.setVisibility(View.VISIBLE);
                mNoMatchingCardsText.setVisibility(View.GONE);
            } else {
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
        } else {
            mCardList.setVisibility(View.GONE);
            mHelpSection.setVisibility(View.VISIBLE);

            mNoMatchingCardsText.setVisibility(View.GONE);
            mNoGroupCardsText.setVisibility(View.GONE);
        }

        if (mCurrentActionMode != null) {
            mCurrentActionMode.finish();
        }
    }

    private void processBarcodeValuesList(List<BarcodeValues> barcodeValuesList, String group, boolean closeAppOnNoBarcode) {
        if (barcodeValuesList.isEmpty()) {
            throw new IllegalArgumentException("barcodesValues may not be empty");
        }

        Utils.makeUserChooseBarcodeFromList(MainActivity.this, barcodeValuesList, new BarcodeValuesListDisambiguatorCallback() {
            @Override
            public void onUserChoseBarcode(BarcodeValues barcodeValues) {
                Intent newIntent = new Intent(getApplicationContext(), LoyaltyCardEditActivity.class);
                Bundle newBundle = new Bundle();
                newBundle.putString(LoyaltyCardEditActivity.BUNDLE_BARCODETYPE, barcodeValues.format());
                newBundle.putString(LoyaltyCardEditActivity.BUNDLE_CARDID, barcodeValues.content());
                if (group != null) {
                    newBundle.putString(LoyaltyCardEditActivity.BUNDLE_ADDGROUP, group);
                }
                newIntent.putExtras(newBundle);
                startActivity(newIntent);
            }

            @Override
            public void onUserDismissedSelector() {
                if (closeAppOnNoBarcode) {
                    finish();
                }
            }
        });
    }

    private void onSharedIntent(Intent intent) {
        String receivedAction = intent.getAction();
        String receivedType = intent.getType();

        // Check if an image or file was shared to us
        if (Intent.ACTION_SEND.equals(receivedAction)) {
            List<BarcodeValues> barcodeValuesList;

            if (receivedType.equals("text/plain")) {
                barcodeValuesList = Collections.singletonList(new BarcodeValues(null, intent.getStringExtra(Intent.EXTRA_TEXT)));
            } else if (receivedType.startsWith("image/")) {
                barcodeValuesList = Utils.retrieveBarcodesFromImage(this, intent.getParcelableExtra(Intent.EXTRA_STREAM));
            } else if (receivedType.equals("application/pdf")) {
                barcodeValuesList = Utils.retrieveBarcodesFromPdf(this, intent.getParcelableExtra(Intent.EXTRA_STREAM));
            } else {
                Log.e(TAG, "Wrong mime-type");
                return;
            }

            if (barcodeValuesList.isEmpty()) {
                finish();
                return;
            }

            processBarcodeValuesList(barcodeValuesList, null, true);
        }
    }

    private void extractIntentFields(Intent intent) {
        onSharedIntent(intent);
    }

    public void updateTabGroups(TabLayout groupsTabLayout) {
        List<Group> newGroups = DBHelper.getGroups(mDatabase);

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
    public boolean onCreateOptionsMenu(Menu inputMenu) {
        getMenuInflater().inflate(R.menu.main_menu, inputMenu);

        displayCardSetupOptions(inputMenu, mLoyaltyCardCount > 0);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null) {
            mSearchView = (SearchView) inputMenu.findItem(R.id.action_search).getActionView();
            mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            mSearchView.setSubmitButtonEnabled(false);

            mSearchView.setOnCloseListener(() -> {
                invalidateOptionsMenu();
                return false;
            });

            mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    mFilter = newText;

                    TabLayout.Tab currentTab = groupsTabLayout.getTabAt(groupsTabLayout.getSelectedTabPosition());
                    mGroup = currentTab != null ? currentTab.getTag() : null;

                    updateLoyaltyCardList(false);

                    return true;
                }
            });
        }

        return super.onCreateOptionsMenu(inputMenu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem inputItem) {
        int id = inputItem.getItemId();

        if (id == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
        }

        if (id == R.id.action_display_options) {
            mAdapter.showDisplayOptionsDialog();
            invalidateOptionsMenu();

            return true;
        }

        if (id == R.id.action_sort) {
            AtomicInteger currentIndex = new AtomicInteger();
            List<DBHelper.LoyaltyCardOrder> loyaltyCardOrders = Arrays.asList(DBHelper.LoyaltyCardOrder.values());
            for (int i = 0; i < loyaltyCardOrders.size(); i++) {
                if (mOrder == loyaltyCardOrders.get(i)) {
                    currentIndex.set(i);
                    break;
                }
            }

            AlertDialog.Builder builder = new MaterialAlertDialogBuilder(MainActivity.this);
            builder.setTitle(R.string.sort_by);

            SortingOptionBinding sortingOptionBinding = SortingOptionBinding
                    .inflate(LayoutInflater.from(MainActivity.this), null, false);
            final View customLayout = sortingOptionBinding.getRoot();
            builder.setView(customLayout);

            CheckBox showReversed = sortingOptionBinding.checkBoxReverse;


            showReversed.setChecked(mOrderDirection == DBHelper.LoyaltyCardOrderDirection.Descending);


            builder.setSingleChoiceItems(R.array.sort_types_array, currentIndex.get(), (dialog, which) -> currentIndex.set(which));

            builder.setPositiveButton(R.string.sort, (dialog, which) -> {

                setSort(
                        loyaltyCardOrders.get(currentIndex.get()),
                        showReversed.isChecked() ? DBHelper.LoyaltyCardOrderDirection.Descending : DBHelper.LoyaltyCardOrderDirection.Ascending
                );

                dialog.dismiss();
            });

            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

            AlertDialog dialog = builder.create();
            dialog.show();

            return true;
        }

        if (id == R.id.action_manage_groups) {
            Intent i = new Intent(getApplicationContext(), ManageGroupsActivity.class);
            startActivity(i);
            return true;
        }

        if (id == R.id.action_import_export) {
            Intent i = new Intent(getApplicationContext(), ImportExportActivity.class);
            mImportExportLauncher.launch(i);
            return true;
        }

        if (id == R.id.action_settings) {
            Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
            mSettingsLauncher.launch(i);
            return true;
        }

        if (id == R.id.action_about) {
            Intent i = new Intent(getApplicationContext(), AboutActivity.class);
            startActivity(i);
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
        updateLoyaltyCardList(false);
    }

    @Override
    public void onRowLongClicked(int inputPosition) {
        enableActionMode(inputPosition);
    }

    private void enableActionMode(int inputPosition) {
        if (mCurrentActionMode == null) {
            mCurrentActionMode = startSupportActionMode(mCurrentActionModeCallback);
        }
        toggleSelection(inputPosition);
    }

    private void scaleScreen() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;
        float mediumSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,MEDIUM_SCALE_FACTOR_DIP,getResources().getDisplayMetrics());
        boolean shouldScaleSmaller = screenHeight < mediumSizePx;

        binding.include.welcomeIcon.setVisibility(shouldScaleSmaller ? View.GONE : View.VISIBLE);
    }

    private void toggleSelection(int inputPosition) {
        mAdapter.toggleSelection(inputPosition);
        int count = mAdapter.getSelectedItemCount();

        if (count == 0) {
            mCurrentActionMode.finish();
        } else {
            mCurrentActionMode.setTitle(getResources().getQuantityString(R.plurals.selectedCardCount, count, count));

            MenuItem editItem = mCurrentActionMode.getMenu().findItem(R.id.action_edit);
            MenuItem archiveItem = mCurrentActionMode.getMenu().findItem(R.id.action_archive);
            MenuItem unarchiveItem = mCurrentActionMode.getMenu().findItem(R.id.action_unarchive);
            MenuItem starItem = mCurrentActionMode.getMenu().findItem(R.id.action_star);
            MenuItem unstarItem = mCurrentActionMode.getMenu().findItem(R.id.action_unstar);

            boolean hasStarred = false;
            boolean hasUnstarred = false;
            boolean hasArchived = false;
            boolean hasUnarchived = false;

            for (LoyaltyCard loyaltyCard : mAdapter.getSelectedItems()) {
                if (loyaltyCard.starStatus == 1) {
                    hasStarred = true;
                } else {
                    hasUnstarred = true;
                }

                if (loyaltyCard.archiveStatus == 1) {
                    hasArchived = true;
                } else {
                    hasUnarchived = true;
                }

                // We have all types, no need to keep checking
                if (hasStarred && hasUnstarred && hasArchived && hasUnarchived) {
                    break;
                }
            }

            unarchiveItem.setVisible(hasArchived);
            archiveItem.setVisible(hasUnarchived);

            if (count == 1) {
                starItem.setVisible(!hasStarred);
                unstarItem.setVisible(!hasUnstarred);
                editItem.setVisible(true);
                editItem.setEnabled(true);
            } else {
                starItem.setVisible(hasUnstarred);
                unstarItem.setVisible(hasStarred);

                editItem.setVisible(false);
                editItem.setEnabled(false);
            }

            mCurrentActionMode.invalidate();
        }
    }


    @Override
    public void onRowClicked(int inputPosition) {
        if (mAdapter.getSelectedItemCount() > 0) {
            enableActionMode(inputPosition);
        } else {
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
                loyaltyCard = mAdapter.getCard(inputPosition);
            } catch (CursorIndexOutOfBoundsException e) {
                Log.w(TAG, "Prevented crash from tap + swipe on ID " + inputPosition + ": " + e);
                return;
            }

            Intent intent = new Intent(this, LoyaltyCardViewActivity.class);
            intent.setAction("");
            final Bundle b = new Bundle();
            b.putInt("id", loyaltyCard.id);

            ArrayList<Integer> cardList = new ArrayList<>();
            for (int i = 0; i < mAdapter.getItemCount(); i++) {
                cardList.add(mAdapter.getCard(i).id);
            }

            b.putIntegerArrayList("cardList", cardList);
            intent.putExtras(b);

            startActivity(intent);
        }
    }
}
