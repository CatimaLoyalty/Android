package protect.card_locker

import android.app.SearchManager
import android.content.DialogInterface
import android.content.Intent
import android.database.CursorIndexOutOfBoundsException
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import protect.card_locker.DBHelper.LoyaltyCardOrder
import protect.card_locker.DBHelper.LoyaltyCardOrderDirection
import protect.card_locker.LoyaltyCardCursorAdapter.CardAdapterListener
import protect.card_locker.databinding.ContentMainBinding
import protect.card_locker.databinding.MainActivityBinding
import protect.card_locker.databinding.SortingOptionBinding
import protect.card_locker.preferences.Settings
import protect.card_locker.preferences.SettingsActivity
import java.io.UnsupportedEncodingException
import java.util.Arrays
import java.util.concurrent.atomic.AtomicInteger

class MainActivity : CatimaAppCompatActivity(), CardAdapterListener {
    private lateinit var binding: MainActivityBinding
    private lateinit var contentMainBinding: ContentMainBinding
    private lateinit var mDatabase: SQLiteDatabase
    private lateinit var mAdapter: LoyaltyCardCursorAdapter
    private var mCurrentActionMode: ActionMode? = null
    private var mSearchView: SearchView? = null
    private var mLoyaltyCardCount = 0
    @JvmField
    var mFilter: String = ""
    private var currentQuery = ""
    private var finalQuery = ""
    protected var mGroup: Any? = null
    protected var mOrder: LoyaltyCardOrder = LoyaltyCardOrder.Alpha
    protected var mOrderDirection: LoyaltyCardOrderDirection = LoyaltyCardOrderDirection.Ascending
    protected var selectedTab: Int = 0
    private lateinit var groupsTabLayout: TabLayout
    private lateinit var mUpdateLoyaltyCardListRunnable: Runnable
    private lateinit var mBarcodeScannerLauncher: ActivityResultLauncher<Intent?>
    private lateinit var mSettingsLauncher: ActivityResultLauncher<Intent?>

    private val mCurrentActionModeCallback: ActionMode.Callback = object : ActionMode.Callback {
        override fun onCreateActionMode(inputMode: ActionMode, inputMenu: Menu?): Boolean {
            inputMode.getMenuInflater().inflate(R.menu.card_longclick_menu, inputMenu)
            return true
        }

        override fun onPrepareActionMode(inputMode: ActionMode?, inputMenu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(inputMode: ActionMode, inputItem: MenuItem): Boolean {
            when (inputItem.getItemId()) {
                R.id.action_share -> {
                    val importURIHelper = ImportURIHelper(this@MainActivity)
                    try {
                        importURIHelper.startShareIntent(mAdapter.getSelectedItems())
                    } catch (e: UnsupportedEncodingException) {
                        Toast.makeText(
                            this@MainActivity,
                            R.string.failedGeneratingShareURL,
                            Toast.LENGTH_LONG
                        ).show()
                        e.printStackTrace()
                    }
                    inputMode.finish()
                    return true
                }
                R.id.action_edit -> {
                    require(mAdapter.getSelectedItemCount() == 1) { "Cannot edit more than 1 card at a time" }

                    val intent = Intent(getApplicationContext(), LoyaltyCardEditActivity::class.java)
                    val bundle = Bundle()
                    bundle.putInt(
                        LoyaltyCardEditActivity.BUNDLE_ID,
                        mAdapter.getSelectedItems().get(0).id
                    )
                    bundle.putBoolean(LoyaltyCardEditActivity.BUNDLE_UPDATE, true)
                    intent.putExtras(bundle)
                    startActivity(intent)
                    inputMode.finish()
                    return true
                }
                R.id.action_delete -> {
                    val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(this@MainActivity)
                    // The following may seem weird, but it is necessary to give translators enough flexibility.
                    // For example, in Russian, Android's plural quantity "one" actually refers to "any number ending on 1 but not ending in 11".
                    // So while in English the extra non-plural form seems unnecessary duplication, it is necessary to give translators enough flexibility.
                    // In here, we use the plain string when meaning exactly 1, and otherwise use the plural forms
                    if (mAdapter.getSelectedItemCount() == 1) {
                        builder.setTitle(R.string.deleteTitle)
                        builder.setMessage(R.string.deleteConfirmation)
                    } else {
                        builder.setTitle(
                            getResources().getQuantityString(
                                R.plurals.deleteCardsTitle,
                                mAdapter.getSelectedItemCount(),
                                mAdapter.getSelectedItemCount()
                            )
                        )
                        builder.setMessage(
                            getResources().getQuantityString(
                                R.plurals.deleteCardsConfirmation,
                                mAdapter.getSelectedItemCount(),
                                mAdapter.getSelectedItemCount()
                            )
                        )
                    }

                    builder.setPositiveButton(
                        R.string.confirm
                    ) { dialog, _ ->
                        for (loyaltyCard in mAdapter.getSelectedItems()) {
                            Log.d(TAG, "Deleting card: " + loyaltyCard.id)

                            DBHelper.deleteLoyaltyCard(mDatabase, this@MainActivity, loyaltyCard.id)

                            ShortcutHelper.removeShortcut(this@MainActivity, loyaltyCard.id)
                        }
                        val tab = groupsTabLayout.getTabAt(selectedTab)
                        mGroup = if (tab != null) tab.getTag() else null

                        updateLoyaltyCardList(true)
                        dialog.dismiss()
                    }
                    builder.setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    val dialog = builder.create()
                    dialog.show()

                    return true
                }
                R.id.action_archive -> {
                    for (loyaltyCard in mAdapter.getSelectedItems()) {
                        Log.d(TAG, "Archiving card: " + loyaltyCard.id)
                        DBHelper.updateLoyaltyCardArchiveStatus(mDatabase, loyaltyCard.id, 1)
                        ShortcutHelper.removeShortcut(this@MainActivity, loyaltyCard.id)
                        updateLoyaltyCardList(false)
                        inputMode.finish()
                        invalidateOptionsMenu()
                    }
                    return true
                }
                R.id.action_unarchive -> {
                    for (loyaltyCard in mAdapter.getSelectedItems()) {
                        Log.d(TAG, "Unarchiving card: " + loyaltyCard.id)
                        DBHelper.updateLoyaltyCardArchiveStatus(mDatabase, loyaltyCard.id, 0)
                        updateLoyaltyCardList(false)
                        inputMode.finish()
                        invalidateOptionsMenu()
                    }
                    return true
                }
                R.id.action_star -> {
                    for (loyaltyCard in mAdapter.getSelectedItems()) {
                        Log.d(TAG, "Starring card: " + loyaltyCard.id)
                        DBHelper.updateLoyaltyCardStarStatus(mDatabase, loyaltyCard.id, 1)
                        updateLoyaltyCardList(false)
                        inputMode.finish()
                    }
                    return true
                }
                R.id.action_unstar -> {
                    for (loyaltyCard in mAdapter.getSelectedItems()) {
                        Log.d(TAG, "Unstarring card: " + loyaltyCard.id)
                        DBHelper.updateLoyaltyCardStarStatus(mDatabase, loyaltyCard.id, 0)
                        updateLoyaltyCardList(false)
                        inputMode.finish()
                    }
                    return true
                }
            }

            return false
        }

        override fun onDestroyActionMode(inputMode: ActionMode?) {
            mAdapter.clearSelections()
            mCurrentActionMode = null
        }
    }

    override fun onCreate(inputSavedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(inputSavedInstanceState)

        // Delete old cache files
        // These could be temporary images for the cropper, temporary images in LoyaltyCard toBundle/writeParcel/ etc.
        Thread {
            val twentyFourHoursAgo = System.currentTimeMillis() - (1000 * 60 * 60 * 24)
            val tempFiles = getCacheDir().listFiles()

            if (tempFiles == null) {
                Log.e(
                    TAG,
                    "getCacheDir().listFiles() somehow returned null, this should never happen... Skipping cache cleanup..."
                )
                return@Thread
            }
            for (file in tempFiles) {
                if (file.lastModified() < twentyFourHoursAgo) {
                    if (!file.delete()) {
                        Log.w(TAG, "Failed to delete cache file " + file.getPath())
                    }
                }
            }
        }.start()

        // We should extract the share intent after we called the super.onCreate as it may need to spawn a dialog window and the app needs to be initialized to not crash
        extractIntentFields(intent)

        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())
        Utils.applyWindowInsets(binding.getRoot())
        setSupportActionBar(binding.toolbar)
        groupsTabLayout = binding.groups
        contentMainBinding = ContentMainBinding.bind(binding.include.getRoot())

        mDatabase = DBHelper(this).writableDatabase

        mUpdateLoyaltyCardListRunnable = Runnable {
            updateLoyaltyCardList(false)
        }

        groupsTabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                selectedTab = tab.getPosition()
                Log.d("onTabSelected", "Tab Position " + tab.getPosition())
                mGroup = tab.getTag()
                updateLoyaltyCardList(false)
                // Store active tab in Shared Preference to restore next app launch
                val activeTabPref = applicationContext.getSharedPreferences(
                    getString(R.string.sharedpreference_active_tab),
                    MODE_PRIVATE
                )
                val activeTabPrefEditor = activeTabPref.edit()
                activeTabPrefEditor.putInt(
                    getString(R.string.sharedpreference_active_tab),
                    tab.getPosition()
                )
                activeTabPrefEditor.apply()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

        mAdapter = LoyaltyCardCursorAdapter(this, null, this, mUpdateLoyaltyCardListRunnable)
        contentMainBinding.list.setAdapter(mAdapter)
        registerForContextMenu(contentMainBinding.list)

        mBarcodeScannerLauncher = registerForActivityResult<Intent?, ActivityResult?>(
            StartActivityForResult(),
            ActivityResultCallback registerForActivityResult@{ result: ActivityResult? ->
                // Exit early if the user cancelled the scan (pressed back/home)
                if (result!!.getResultCode() != RESULT_OK) {
                    return@registerForActivityResult
                }

                val editIntent =
                    Intent(applicationContext, LoyaltyCardEditActivity::class.java)
                editIntent.putExtras(result.data!!.extras!!)
                startActivity(editIntent)
            })

        mSettingsLauncher = registerForActivityResult<Intent?, ActivityResult?>(
            StartActivityForResult(),
            ActivityResultCallback { result: ActivityResult? ->
                if (result!!.resultCode == RESULT_OK) {
                    val intent = result.data
                    if (intent != null && intent.getBooleanExtra(RESTART_ACTIVITY_INTENT, false)) {
                        recreate()
                    }
                }
            })

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (mSearchView != null && !mSearchView!!.isIconified) {
                    mSearchView!!.isIconified = true
                } else {
                    finish()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()

        if (mCurrentActionMode != null) {
            mAdapter.clearSelections()
            mCurrentActionMode!!.finish()
        }

        if (mSearchView != null && !mSearchView!!.isIconified()) {
            mFilter = mSearchView!!.getQuery().toString()
        }
        // Start of active tab logic
        updateTabGroups(groupsTabLayout)

        // Restore selected tab from Shared Preference
        val activeTabPref = getApplicationContext().getSharedPreferences(
            getString(R.string.sharedpreference_active_tab),
            MODE_PRIVATE
        )
        selectedTab = activeTabPref.getInt(getString(R.string.sharedpreference_active_tab), 0)

        // Restore sort preferences from Shared Preferences
        mOrder = Utils.getLoyaltyCardOrder(this)
        mOrderDirection = Utils.getLoyaltyCardOrderDirection(this)

        mGroup = null

        if (groupsTabLayout.getTabCount() != 0) {
            var tab = groupsTabLayout.getTabAt(selectedTab)
            if (tab == null) {
                tab = groupsTabLayout.getTabAt(0)
            }

            groupsTabLayout.selectTab(tab)
            checkNotNull(tab)
            mGroup = tab.getTag()
        } else {
            scaleScreen()
        }

        updateLoyaltyCardList(true)

        binding.fabAdd.setOnClickListener {
            val intent = Intent(getApplicationContext(), ScanActivity::class.java)
            val bundle = Bundle()
            if (selectedTab != 0) {
                bundle.putString(
                    LoyaltyCardEditActivity.BUNDLE_ADDGROUP,
                    groupsTabLayout.getTabAt(selectedTab)!!.getText().toString()
                )
            }
            intent.putExtras(bundle)
            mBarcodeScannerLauncher.launch(intent)
        }
        // End of active tab logic
        binding.fabAdd.bringToFront()

        val layoutManager = contentMainBinding.list.getLayoutManager() as GridLayoutManager?
        if (layoutManager != null) {
            val settings = Settings(this)
            layoutManager.setSpanCount(settings.getPreferredColumnCount())
        }
    }

    private fun displayCardSetupOptions(menu: Menu, shouldShow: Boolean) {
        for (id in intArrayOf(R.id.action_search, R.id.action_display_options, R.id.action_sort)) {
            menu.findItem(id).setVisible(shouldShow)
        }
    }

    private fun updateLoyaltyCardCount() {
        mLoyaltyCardCount = DBHelper.getLoyaltyCardCount(mDatabase)
    }

    private fun updateLoyaltyCardList(updateCount: Boolean) {
        var group: Group? = null
        if (mGroup != null) {
            group = mGroup as Group
        }

        mAdapter.swapCursor(
            DBHelper.getLoyaltyCardCursor(
                mDatabase,
                mFilter,
                group,
                mOrder,
                mOrderDirection,
                if (mAdapter.showingArchivedCards()) DBHelper.LoyaltyCardArchiveFilter.All else DBHelper.LoyaltyCardArchiveFilter.Unarchived
            )
        )

        if (updateCount) {
            updateLoyaltyCardCount()
            // Update menu icons if necessary
            invalidateOptionsMenu()
        }

        if (mLoyaltyCardCount > 0) {
            // We want the cardList to be visible regardless of the filtered match count
            // to ensure that the noMatchingCardsText doesn't end up being shown below
            // the keyboard
            contentMainBinding.helpSection.setVisibility(View.GONE)
            contentMainBinding.noGroupCardsText.setVisibility(View.GONE)

            if (mAdapter.getItemCount() > 0) {
                contentMainBinding.list.setVisibility(View.VISIBLE)
                contentMainBinding.noMatchingCardsText.setVisibility(View.GONE)
            } else {
                contentMainBinding.list.setVisibility(View.GONE)
                if (!mFilter.isEmpty()) {
                    // Actual Empty Search Result
                    contentMainBinding.noMatchingCardsText.setVisibility(View.VISIBLE)
                    contentMainBinding.noGroupCardsText.setVisibility(View.GONE)
                } else {
                    // Group Tab with no Group Cards
                    contentMainBinding.noMatchingCardsText.setVisibility(View.GONE)
                    contentMainBinding.noGroupCardsText.setVisibility(View.VISIBLE)
                }
            }
        } else {
            contentMainBinding.list.setVisibility(View.GONE)
            contentMainBinding.helpSection.setVisibility(View.VISIBLE)

            contentMainBinding.noMatchingCardsText.setVisibility(View.GONE)
            contentMainBinding.noGroupCardsText.setVisibility(View.GONE)
        }

        if (mCurrentActionMode != null) {
            mCurrentActionMode!!.finish()
        }

        ListWidget().updateAll(mAdapter.mContext)
    }

    private fun processParseResultList(
        parseResultList: MutableList<ParseResult?>,
        group: String?,
        closeAppOnNoBarcode: Boolean
    ) {
        require(!parseResultList.isEmpty()) { "parseResultList may not be empty" }

        Utils.makeUserChooseParseResultFromList(
            this@MainActivity,
            parseResultList,
            object : ParseResultListDisambiguatorCallback {
                override fun onUserChoseParseResult(parseResult: ParseResult) {
                    val intent =
                        Intent(getApplicationContext(), LoyaltyCardEditActivity::class.java)
                    val bundle = parseResult.toLoyaltyCardBundle(this@MainActivity)
                    if (group != null) {
                        bundle.putString(LoyaltyCardEditActivity.BUNDLE_ADDGROUP, group)
                    }
                    intent.putExtras(bundle)
                    startActivity(intent)
                }

                override fun onUserDismissedSelector() {
                    if (closeAppOnNoBarcode) {
                        finish()
                    }
                }
            })
    }

    private fun onSharedIntent(intent: Intent) {
        val receivedAction = intent.getAction()
        val receivedType = intent.getType()

        if (receivedAction == null || receivedType == null) {
            return
        }

        val parseResultList: MutableList<ParseResult?>?

        // Check for shared text
        if (receivedAction == Intent.ACTION_SEND && receivedType == "text/plain") {
            val loyaltyCard = LoyaltyCard()
            loyaltyCard.setCardId(intent.getStringExtra(Intent.EXTRA_TEXT)!!)
            parseResultList =
                mutableListOf<ParseResult?>(ParseResult(ParseResultType.BARCODE_ONLY, loyaltyCard))
        } else {
            // Parse whatever file was sent, regardless of opening or sharing
            val data: Uri?
            if (receivedAction == Intent.ACTION_VIEW) {
                data = intent.data
            } else if (receivedAction == Intent.ACTION_SEND) {
                data = intent.getParcelableExtra<Uri?>(Intent.EXTRA_STREAM)
            } else {
                Log.e(TAG, "Wrong action type to parse intent")
                return
            }

            if (receivedType.startsWith("image/")) {
                parseResultList = Utils.retrieveBarcodesFromImage(this, data)
            } else if (receivedType == "application/pdf") {
                parseResultList = Utils.retrieveBarcodesFromPdf(this, data)
            } else if (mutableListOf<String?>(
                    "application/vnd.apple.pkpass",
                    "application/vnd-com.apple.pkpass"
                ).contains(receivedType)
            ) {
                parseResultList = Utils.retrieveBarcodesFromPkPass(this, data)
            } else if (receivedType == "application/vnd.espass-espass") {
                // FIXME: espass is not pkpass
                // However, several users stated in https://github.com/CatimaLoyalty/Android/issues/2197 that the formats are extremely similar to the point they could rename an .espass file to .pkpass and have it imported
                // So it makes sense to "unofficially" treat it as a PKPASS for now, even though not completely correct
                parseResultList = Utils.retrieveBarcodesFromPkPass(this, data)
            } else if (receivedType == "application/vnd.apple.pkpasses") {
                parseResultList = Utils.retrieveBarcodesFromPkPasses(this, data)
            } else {
                Log.e(TAG, "Wrong mime-type")
                return
            }
        }

        // Give up if we should parse but there is nothing to parse
        if (parseResultList == null || parseResultList.isEmpty()) {
            finish()
            return
        }

        processParseResultList(parseResultList, null, true)
    }

    private fun extractIntentFields(intent: Intent) {
        onSharedIntent(intent)
    }

    fun updateTabGroups(groupsTabLayout: TabLayout) {
        val newGroups = DBHelper.getGroups(mDatabase)

        if (newGroups.size == 0) {
            groupsTabLayout.removeAllTabs()
            groupsTabLayout.setVisibility(View.GONE)
            return
        }

        groupsTabLayout.removeAllTabs()

        val allTab = groupsTabLayout.newTab()
        allTab.setText(R.string.all)
        allTab.setTag(null)
        groupsTabLayout.addTab(allTab, false)

        for (group in newGroups) {
            val tab = groupsTabLayout.newTab()
            tab.setText(group._id)
            tab.setTag(group)
            groupsTabLayout.addTab(tab, false)
        }

        groupsTabLayout.setVisibility(View.VISIBLE)
    }

    // Saving currentQuery to finalQuery for user, this will be used to restore search history, happens when user clicks a card from list
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        finalQuery = currentQuery
        // Putting the query also into outState for later use in onRestoreInstanceState when rotating screen
        if (mSearchView != null) {
            outState.putString(STATE_SEARCH_QUERY, finalQuery)
        }
    }

    // Restoring instance state when rotation of screen happens with the goal to restore search query for user
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        finalQuery = savedInstanceState.getString(STATE_SEARCH_QUERY, "")
    }

    override fun onCreateOptionsMenu(inputMenu: Menu): Boolean {
        getMenuInflater().inflate(R.menu.main_menu, inputMenu)

        displayCardSetupOptions(inputMenu, mLoyaltyCardCount > 0)

        val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager?
        if (searchManager != null) {
            val searchMenuItem = inputMenu.findItem(R.id.action_search)
            mSearchView = searchMenuItem.getActionView() as SearchView?
            mSearchView!!.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()))
            mSearchView!!.setSubmitButtonEnabled(false)
            mSearchView!!.setOnCloseListener(SearchView.OnCloseListener {
                invalidateOptionsMenu()
                false
            })

            /*
             * On Android 13 and later, pressing Back while the search view is open hides the keyboard
             * and collapses the search view at the same time.
             * This brings back the old behavior on Android 12 and lower: pressing Back once
             * hides the keyboard, press again while keyboard is hidden to collapse the search view.
             */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                        return true
                    }

                    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                        if (mSearchView!!.hasFocus()) {
                            mSearchView!!.clearFocus()
                            return false
                        }
                        currentQuery = ""
                        mFilter = ""
                        updateLoyaltyCardList(false)
                        return true
                    }
                })
            }

            mSearchView!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    mFilter = newText
                    // New logic to ensure search history after coming back from picked card - user will see the last search query
                    if (newText.isEmpty()) {
                        if (!finalQuery.isEmpty()) {
                            // Setting the query text for user after coming back from picked card from finalQuery
                            mSearchView!!.setQuery(finalQuery, false)
                        } else if (!currentQuery.isEmpty()) {
                            // Else if is needed in case user deletes search - expected behaviour is to show all cards
                            currentQuery = ""
                            mSearchView!!.setQuery(currentQuery, false)
                        }
                    } else {
                        // Setting search query each time user changes the text in search to temporary variable to be used later in finalQuery String which will be used to restore search history
                        currentQuery = newText
                    }
                    val currentTab =
                        groupsTabLayout.getTabAt(groupsTabLayout.getSelectedTabPosition())
                    mGroup = if (currentTab != null) currentTab.getTag() else null

                    updateLoyaltyCardList(false)

                    return true
                }
            })
            // Check if we came from a picked card back to search, in that case we want to show the search view with previous search query
            if (!finalQuery.isEmpty()) {
                // Expand the search view to show the query
                searchMenuItem.expandActionView()
                // Setting the query text to empty String due to behaviour of onQueryTextChange after coming back from picked card - onQueryTextChange is called automatically without users interaction
                finalQuery = ""
                mSearchView!!.setQuery(currentQuery, false)
            }
        }

        return super.onCreateOptionsMenu(inputMenu)
    }

    override fun onOptionsItemSelected(inputItem: MenuItem): Boolean {
        when (val id = inputItem.getItemId()) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
            }
            protect.card_locker.R.id.action_display_options -> {
                mAdapter.showDisplayOptionsDialog()
                invalidateOptionsMenu()
                return true
            }
            protect.card_locker.R.id.action_sort -> {
            val currentIndex = AtomicInteger()
            val loyaltyCardOrders =
                listOf<LoyaltyCardOrder?>(*LoyaltyCardOrder.entries.toTypedArray())
            for (i in loyaltyCardOrders.indices) {
                if (mOrder == loyaltyCardOrders.get(i)) {
                    currentIndex.set(i)
                    break
                }
            }

            val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(this@MainActivity)
            builder.setTitle(protect.card_locker.R.string.sort_by)

            val sortingOptionBinding = SortingOptionBinding
                .inflate(LayoutInflater.from(this@MainActivity), null, false)
            val customLayout: View = sortingOptionBinding.getRoot()
            builder.setView(customLayout)

            val showReversed = sortingOptionBinding.checkBoxReverse


            showReversed.setChecked(mOrderDirection == LoyaltyCardOrderDirection.Descending)


            builder.setSingleChoiceItems(
                protect.card_locker.R.array.sort_types_array,
                currentIndex.get(),
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                    currentIndex.set(which)
                })

            builder.setPositiveButton(
                protect.card_locker.R.string.sort,
                DialogInterface.OnClickListener { dialog, _ ->
                    setSort(
                        loyaltyCardOrders.get(currentIndex.get())!!,
                        if (showReversed.isChecked) LoyaltyCardOrderDirection.Descending else LoyaltyCardOrderDirection.Ascending
                    )
                    ListWidget().updateAll(this)
                    dialog?.dismiss()
                }
            )

            builder.setNegativeButton(protect.card_locker.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()

                return true
            }
            protect.card_locker.R.id.action_manage_groups -> {
                val i = Intent(applicationContext, ManageGroupsActivity::class.java)
                startActivity(i)
                return true
            }
            protect.card_locker.R.id.action_import_export -> {
                val i = Intent(applicationContext, ImportExportActivity::class.java)
                startActivity(i)
                return true
            }
            protect.card_locker.R.id.action_settings -> {
                val i = Intent(applicationContext, SettingsActivity::class.java)
                mSettingsLauncher.launch(i)
                return true
            }
            protect.card_locker.R.id.action_about -> {
                val i = Intent(applicationContext, AboutActivity::class.java)
                startActivity(i)
                return true
            }
        }

        return super.onOptionsItemSelected(inputItem)
    }

    private fun setSort(order: LoyaltyCardOrder, direction: LoyaltyCardOrderDirection) {
        // Update values
        mOrder = order
        mOrderDirection = direction

        // Store in Shared Preference to restore next app launch
        val sortPref = getApplicationContext().getSharedPreferences(
            getString(protect.card_locker.R.string.sharedpreference_sort),
            MODE_PRIVATE
        )
        val sortPrefEditor = sortPref.edit()
        sortPrefEditor.putString(
            getString(protect.card_locker.R.string.sharedpreference_sort_order),
            order.name
        )
        sortPrefEditor.putString(
            getString(protect.card_locker.R.string.sharedpreference_sort_direction),
            direction.name
        )
        sortPrefEditor.apply()

        // Update card list
        updateLoyaltyCardList(false)
    }

    override fun onRowLongClicked(inputPosition: Int) {
        enableActionMode(inputPosition)
    }

    private fun enableActionMode(inputPosition: Int) {
        if (mCurrentActionMode == null) {
            mCurrentActionMode = startSupportActionMode(mCurrentActionModeCallback)
        }
        toggleSelection(inputPosition)
    }

    private fun scaleScreen() {
        val displayMetrics = DisplayMetrics()
        windowManager.getDefaultDisplay().getMetrics(displayMetrics)
        val screenHeight = displayMetrics.heightPixels
        val mediumSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            MEDIUM_SCALE_FACTOR_DIP.toFloat(),
            getResources().displayMetrics
        )
        val shouldScaleSmaller = screenHeight < mediumSizePx

        binding.include.welcomeIcon.visibility = if (shouldScaleSmaller) View.GONE else View.VISIBLE
    }

    private fun toggleSelection(inputPosition: Int) {
        mAdapter.toggleSelection(inputPosition)
        val count = mAdapter.selectedItemCount

        if (count == 0) {
            mCurrentActionMode!!.finish()
        } else {
            mCurrentActionMode!!.setTitle(
                getResources().getQuantityString(
                    protect.card_locker.R.plurals.selectedCardCount,
                    count,
                    count
                )
            )

            val editItem =
                mCurrentActionMode!!.menu.findItem(protect.card_locker.R.id.action_edit)
            val archiveItem =
                mCurrentActionMode!!.menu.findItem(protect.card_locker.R.id.action_archive)
            val unarchiveItem =
                mCurrentActionMode!!.menu.findItem(protect.card_locker.R.id.action_unarchive)
            val starItem =
                mCurrentActionMode!!.menu.findItem(protect.card_locker.R.id.action_star)
            val unstarItem =
                mCurrentActionMode!!.menu.findItem(protect.card_locker.R.id.action_unstar)

            var hasStarred = false
            var hasUnstarred = false
            var hasArchived = false
            var hasUnarchived = false

            for (loyaltyCard in mAdapter.getSelectedItems()) {
                if (loyaltyCard.starStatus == 1) {
                    hasStarred = true
                } else {
                    hasUnstarred = true
                }

                if (loyaltyCard.archiveStatus == 1) {
                    hasArchived = true
                } else {
                    hasUnarchived = true
                }

                // We have all types, no need to keep checking
                if (hasStarred && hasUnstarred && hasArchived && hasUnarchived) {
                    break
                }
            }

            unarchiveItem.setVisible(hasArchived)
            archiveItem.setVisible(hasUnarchived)

            if (count == 1) {
                starItem.setVisible(!hasStarred)
                unstarItem.setVisible(!hasUnstarred)
                editItem.setVisible(true)
                editItem.setEnabled(true)
            } else {
                starItem.setVisible(hasUnstarred)
                unstarItem.setVisible(hasStarred)

                editItem.setVisible(false)
                editItem.setEnabled(false)
            }

            mCurrentActionMode!!.invalidate()
        }
    }


    override fun onRowClicked(inputPosition: Int) {
        if (mAdapter.getSelectedItemCount() > 0) {
            enableActionMode(inputPosition)
        } else {
            // FIXME
            //
            // There is a really nasty edge case that can happen when someone taps a card but right
            // after it swipes (very small window, hard to reproduce). The cursor gets replaced and
            // may not have a card at the ID number that is returned from onRowClicked.
            //
            // The proper fix, obviously, would involve makes sure an onFling can't happen while a
            // click is being processed. Sadly, I have not yet found a way to make that possible.
            val loyaltyCard: LoyaltyCard
            try {
                loyaltyCard = mAdapter.getCard(inputPosition)
            } catch (e: CursorIndexOutOfBoundsException) {
                Log.w(TAG, "Prevented crash from tap + swipe on ID " + inputPosition + ": " + e)
                return
            }

            val intent = Intent(this, LoyaltyCardViewActivity::class.java)
            intent.setAction("")
            val b = Bundle()
            b.putInt(LoyaltyCardViewActivity.BUNDLE_ID, loyaltyCard.id)

            val cardList = ArrayList<Int?>()
            for (i in 0..<mAdapter.getItemCount()) {
                cardList.add(mAdapter.getCard(i).id)
            }

            b.putIntegerArrayList(LoyaltyCardViewActivity.BUNDLE_CARDLIST, cardList)
            intent.putExtras(b)

            startActivity(intent)
        }
    }

    companion object {
        private const val TAG = "Catima"
        const val RESTART_ACTIVITY_INTENT: String = "restart_activity_intent"

        private const val MEDIUM_SCALE_FACTOR_DIP = 460
        const val STATE_SEARCH_QUERY: String = "SEARCH_QUERY"
    }
}
