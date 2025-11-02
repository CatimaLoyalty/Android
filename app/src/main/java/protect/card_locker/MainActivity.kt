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
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.recyclerview.widget.GridLayoutManager
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
import java.util.concurrent.atomic.AtomicInteger
import androidx.core.content.edit

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
    private var mGroup: Any? = null
    private var mOrder: LoyaltyCardOrder = LoyaltyCardOrder.Alpha
    private var mOrderDirection: LoyaltyCardOrderDirection = LoyaltyCardOrderDirection.Ascending
    private var selectedTab: Int = 0
    private lateinit var groupsTabLayout: TabLayout
    private lateinit var mUpdateLoyaltyCardListRunnable: Runnable
    private lateinit var mBarcodeScannerLauncher: ActivityResultLauncher<Intent?>
    private lateinit var mSettingsLauncher: ActivityResultLauncher<Intent?>

    private val mCurrentActionModeCallback: ActionMode.Callback = object : ActionMode.Callback {
        override fun onCreateActionMode(inputMode: ActionMode, inputMenu: Menu?): Boolean {
            inputMode.menuInflater.inflate(R.menu.card_longclick_menu, inputMenu)
            return true
        }

        override fun onPrepareActionMode(inputMode: ActionMode?, inputMenu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(inputMode: ActionMode, inputItem: MenuItem): Boolean {
            when (inputItem.itemId) {
                R.id.action_share -> {
                    try {
                        ImportURIHelper(this@MainActivity).startShareIntent(mAdapter.getSelectedItems())
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
                    require(mAdapter.selectedItemCount == 1) { "Cannot edit more than 1 card at a time" }

                    startActivity(
                        Intent(applicationContext, LoyaltyCardEditActivity::class.java).apply {
                            putExtras(Bundle().apply {
                                putInt(
                                    LoyaltyCardEditActivity.BUNDLE_ID,
                                    mAdapter.getSelectedItems()[0].id
                                )
                                putBoolean(LoyaltyCardEditActivity.BUNDLE_UPDATE, true)
                            })
                        }
                    )

                    inputMode.finish()
                    return true
                }
                R.id.action_duplicate -> {
                    require(mAdapter.selectedItemCount == 1) { "Cannot duplicate more than 1 card at a time" }

                    startActivity(
                        Intent(applicationContext, LoyaltyCardEditActivity::class.java).apply {
                            putExtras(Bundle().apply {
                                putInt(
                                    LoyaltyCardEditActivity.BUNDLE_ID,
                                    mAdapter.getSelectedItems()[0].id
                                )
                                putBoolean(LoyaltyCardEditActivity.BUNDLE_DUPLICATE_ID, true)
                            })
                        }
                    )

                    inputMode.finish()
                    return true
                }
                R.id.action_delete -> {
                    MaterialAlertDialogBuilder(this@MainActivity).apply {
                        // The following may seem weird, but it is necessary to give translators enough flexibility.
                        // For example, in Russian, Android's plural quantity "one" actually refers to "any number ending on 1 but not ending in 11".
                        // So while in English the extra non-plural form seems unnecessary duplication, it is necessary to give translators enough flexibility.
                        // In here, we use the plain string when meaning exactly 1, and otherwise use the plural forms
                        if (mAdapter.selectedItemCount == 1) {
                            setTitle(R.string.deleteTitle)
                            setMessage(R.string.deleteConfirmation)
                        } else {
                            setTitle(
                                getResources().getQuantityString(
                                    R.plurals.deleteCardsTitle,
                                    mAdapter.selectedItemCount,
                                    mAdapter.selectedItemCount
                                )
                            )
                            setMessage(
                                getResources().getQuantityString(
                                    R.plurals.deleteCardsConfirmation,
                                    mAdapter.selectedItemCount,
                                    mAdapter.selectedItemCount
                                )
                            )
                        }

                        setPositiveButton(
                            R.string.confirm
                        ) { dialog, _ ->
                            for (loyaltyCard in mAdapter.getSelectedItems()) {
                                Log.d(TAG, "Deleting card: " + loyaltyCard.id)

                                DBHelper.deleteLoyaltyCard(mDatabase, this@MainActivity, loyaltyCard.id)

                                ShortcutHelper.removeShortcut(this@MainActivity, loyaltyCard.id)
                            }
                            val tab = groupsTabLayout.getTabAt(selectedTab)
                            mGroup = tab?.tag

                            updateLoyaltyCardList(true)
                            dialog.dismiss()
                        }

                        setNegativeButton(R.string.cancel) { dialog, _ ->
                            dialog.dismiss()
                        }
                    }.create().show()

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
            val tempFiles = cacheDir.listFiles()

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
                        Log.w(TAG, "Failed to delete cache file " + file.path)
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
                selectedTab = tab.position
                Log.d("onTabSelected", "Tab Position " + tab.position)
                mGroup = tab.tag
                updateLoyaltyCardList(false)
                // Store active tab in Shared Preference to restore next app launch
                applicationContext.getSharedPreferences(
                    getString(R.string.sharedpreference_active_tab),
                    MODE_PRIVATE
                ).edit {
                    putInt(
                        getString(R.string.sharedpreference_active_tab),
                        tab.position
                    )
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

        mAdapter = LoyaltyCardCursorAdapter(this, null, this, mUpdateLoyaltyCardListRunnable)
        contentMainBinding.list.setAdapter(mAdapter)
        registerForContextMenu(contentMainBinding.list)

        mBarcodeScannerLauncher = registerForActivityResult(
            StartActivityForResult(),
            ActivityResultCallback registerForActivityResult@{ result: ActivityResult? ->
                // Exit early if the user cancelled the scan (pressed back/home)
                if (result == null || result.resultCode != RESULT_OK) {
                    return@registerForActivityResult
                }

                startActivity(
                    Intent(applicationContext, LoyaltyCardEditActivity::class.java).apply {
                        putExtras(result.data!!.extras!!)
                    }
                )
            })

        mSettingsLauncher = registerForActivityResult(
            StartActivityForResult()
        ) { result: ActivityResult? ->
            if (result?.resultCode == RESULT_OK) {
                val intent = result.data
                if (intent != null && intent.getBooleanExtra(RESTART_ACTIVITY_INTENT, false)) {
                    recreate()
                }
            }
        }

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

        if (mSearchView != null && !mSearchView!!.isIconified) {
            mFilter = mSearchView!!.query.toString()
        }
        // Start of active tab logic
        updateTabGroups(groupsTabLayout)

        // Restore selected tab from Shared Preference
        selectedTab = applicationContext.getSharedPreferences(
            getString(R.string.sharedpreference_active_tab),
            MODE_PRIVATE
        ).getInt(getString(R.string.sharedpreference_active_tab), 0)

        // Restore sort preferences from Shared Preferences
        mOrder = Utils.getLoyaltyCardOrder(this)
        mOrderDirection = Utils.getLoyaltyCardOrderDirection(this)

        mGroup = null

        if (groupsTabLayout.tabCount != 0) {
            var tab = groupsTabLayout.getTabAt(selectedTab)
            if (tab == null) {
                tab = groupsTabLayout.getTabAt(0)
            }

            groupsTabLayout.selectTab(tab)
            checkNotNull(tab)
            mGroup = tab.tag
        } else {
            scaleScreen()
        }

        updateLoyaltyCardList(true)

        // End of active tab logic

        binding.fabAdd.setOnClickListener {
            mBarcodeScannerLauncher.launch(
                Intent(applicationContext, ScanActivity::class.java).apply {
                    putExtras(Bundle().apply {
                        if (selectedTab != 0) {
                            putString(
                                LoyaltyCardEditActivity.BUNDLE_ADDGROUP,
                                groupsTabLayout.getTabAt(selectedTab)!!.text.toString()
                            )
                        }
                    })
                }
            )
        }
        binding.fabAdd.bringToFront()

        val layoutManager = contentMainBinding.list.layoutManager as GridLayoutManager?
        if (layoutManager != null) {
            val settings = Settings(this)
            layoutManager.setSpanCount(settings.getPreferredColumnCount())
        }
    }

    private fun displayCardSetupOptions(menu: Menu, shouldShow: Boolean) {
        for (id in intArrayOf(R.id.action_search, R.id.action_display_options, R.id.action_sort)) {
            menu.findItem(id).isVisible = shouldShow
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
            contentMainBinding.helpSection.visibility = View.GONE
            contentMainBinding.noGroupCardsText.visibility = View.GONE

            if (mAdapter.itemCount > 0) {
                contentMainBinding.list.visibility = View.VISIBLE
                contentMainBinding.noMatchingCardsText.visibility = View.GONE
            } else {
                contentMainBinding.list.visibility = View.GONE
                if (!mFilter.isEmpty()) {
                    // Actual Empty Search Result
                    contentMainBinding.noMatchingCardsText.visibility = View.VISIBLE
                    contentMainBinding.noGroupCardsText.visibility = View.GONE
                } else {
                    // Group Tab with no Group Cards
                    contentMainBinding.noMatchingCardsText.visibility = View.GONE
                    contentMainBinding.noGroupCardsText.visibility = View.VISIBLE
                }
            }
        } else {
            contentMainBinding.list.visibility = View.GONE
            contentMainBinding.helpSection.visibility = View.VISIBLE

            contentMainBinding.noMatchingCardsText.visibility = View.GONE
            contentMainBinding.noGroupCardsText.visibility = View.GONE
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
                        Intent(applicationContext, LoyaltyCardEditActivity::class.java)
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
        val receivedAction = intent.action
        val receivedType = intent.type

        if (receivedAction == null || receivedType == null) {
            return
        }

        val parseResultList: MutableList<ParseResult?>?

        // Check for shared text
        if (receivedAction == Intent.ACTION_SEND && receivedType == "text/plain") {
            val loyaltyCard = LoyaltyCard()
            loyaltyCard.setCardId(intent.getStringExtra(Intent.EXTRA_TEXT)!!)
            parseResultList = mutableListOf(ParseResult(ParseResultType.BARCODE_ONLY, loyaltyCard))
        } else {
            // Parse whatever file was sent, regardless of opening or sharing
            val data: Uri? = when (receivedAction) {
                Intent.ACTION_VIEW -> {
                    intent.data
                }
                Intent.ACTION_SEND -> {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                else -> {
                    Log.e(TAG, "Wrong action type to parse intent")
                    return
                }
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

        if (newGroups.isEmpty()) {
            groupsTabLayout.removeAllTabs()
            groupsTabLayout.visibility = View.GONE
            return
        }

        groupsTabLayout.removeAllTabs()
        groupsTabLayout.addTab(
            groupsTabLayout.newTab().apply {
                setText(R.string.all)
                tag = null
            },
            false
        )

        for (group in newGroups) {
            groupsTabLayout.addTab(
                groupsTabLayout.newTab().apply {
                    text = group._id
                    tag = group
                },
                false
            )
        }

        groupsTabLayout.visibility = View.VISIBLE
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
        menuInflater.inflate(R.menu.main_menu, inputMenu)

        displayCardSetupOptions(inputMenu, mLoyaltyCardCount > 0)

        val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager?
        if (searchManager != null) {
            val searchMenuItem = inputMenu.findItem(R.id.action_search)
            mSearchView = searchMenuItem.actionView as SearchView?
            mSearchView!!.setSearchableInfo(searchManager.getSearchableInfo(componentName))
            mSearchView!!.setSubmitButtonEnabled(false)
            mSearchView!!.setOnCloseListener {
                invalidateOptionsMenu()
                false
            }

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
                        groupsTabLayout.getTabAt(groupsTabLayout.selectedTabPosition)
                    mGroup = currentTab?.tag

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
        when (inputItem.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
            }
            R.id.action_display_options -> {
                mAdapter.showDisplayOptionsDialog()
                invalidateOptionsMenu()
                return true
            }
            R.id.action_sort -> {
                val currentIndex = AtomicInteger()
                val loyaltyCardOrders = listOf<LoyaltyCardOrder?>(*LoyaltyCardOrder.entries.toTypedArray())
                for (i in loyaltyCardOrders.indices) {
                    if (mOrder == loyaltyCardOrders[i]) {
                        currentIndex.set(i)
                        break
                    }
                }

                MaterialAlertDialogBuilder(this@MainActivity).apply {
                    setTitle(R.string.sort_by)

                    val sortingOptionBinding = SortingOptionBinding.inflate(LayoutInflater.from(this@MainActivity), null, false)
                    val customLayout: View = sortingOptionBinding.getRoot()
                    setView(customLayout)

                    val showReversed = sortingOptionBinding.checkBoxReverse

                    showReversed.isChecked = mOrderDirection == LoyaltyCardOrderDirection.Descending

                    setSingleChoiceItems(
                        R.array.sort_types_array,
                        currentIndex.get()
                    ) { _: DialogInterface?, which: Int ->
                        currentIndex.set(which)
                    }

                    setPositiveButton(
                        R.string.sort
                    ) { dialog, _ ->
                        setSort(
                            loyaltyCardOrders[currentIndex.get()]!!,
                            if (showReversed.isChecked) LoyaltyCardOrderDirection.Descending else LoyaltyCardOrderDirection.Ascending
                        )
                        ListWidget().updateAll(this@MainActivity)
                        dialog?.dismiss()
                    }

                    setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                }.create().show()

                return true
            }
            R.id.action_manage_groups -> {
                startActivity(
                    Intent(applicationContext, ManageGroupsActivity::class.java)
                )
                return true
            }
            R.id.action_import_export -> {
                startActivity(
                    Intent(applicationContext, ImportExportActivity::class.java)
                )
                return true
            }
            R.id.action_settings -> {
                mSettingsLauncher.launch(
                    Intent(applicationContext, SettingsActivity::class.java)
                )
                return true
            }
            R.id.action_about -> {
                startActivity(
                    Intent(applicationContext, AboutActivity::class.java)
                )
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
        applicationContext.getSharedPreferences(
            getString(R.string.sharedpreference_sort),
            MODE_PRIVATE
        ).edit {
            putString(
                getString(R.string.sharedpreference_sort_order),
                order.name
            )
            putString(
                getString(R.string.sharedpreference_sort_direction),
                direction.name
            )
        }

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
        windowManager.defaultDisplay.getMetrics(displayMetrics)
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
            mCurrentActionMode!!.title = getResources().getQuantityString(
                R.plurals.selectedCardCount,
                count,
                count
            )

            val editItem = mCurrentActionMode!!.menu.findItem(R.id.action_edit)
            val duplicateItem = mCurrentActionMode!!.menu.findItem(R.id.action_duplicate)
            val archiveItem = mCurrentActionMode!!.menu.findItem(R.id.action_archive)
            val unarchiveItem = mCurrentActionMode!!.menu.findItem(R.id.action_unarchive)
            val starItem = mCurrentActionMode!!.menu.findItem(R.id.action_star)
            val unstarItem = mCurrentActionMode!!.menu.findItem(R.id.action_unstar)

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

            unarchiveItem.isVisible = hasArchived
            archiveItem.isVisible = hasUnarchived

            if (count == 1) {
                starItem.isVisible = !hasStarred
                unstarItem.isVisible = !hasUnstarred
                editItem.isVisible = true
                editItem.isEnabled = true
                duplicateItem.isVisible = true
                duplicateItem.isEnabled = true
            } else {
                starItem.isVisible = hasUnstarred
                unstarItem.isVisible = hasStarred

                editItem.isVisible = false
                editItem.isEnabled = false
                duplicateItem.isVisible = false
                duplicateItem.isEnabled = false
            }

            mCurrentActionMode!!.invalidate()
        }
    }


    override fun onRowClicked(inputPosition: Int) {
        if (mAdapter.selectedItemCount > 0) {
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
                Log.w(TAG, "Prevented crash from tap + swipe on ID $inputPosition: $e")
                return
            }

            startActivity(
                Intent(this, LoyaltyCardViewActivity::class.java).apply {
                    action = ""
                    putExtras(Bundle().apply {
                        putInt(LoyaltyCardViewActivity.BUNDLE_ID, loyaltyCard.id)

                        val cardList = ArrayList<Int?>()
                        for (i in 0..<mAdapter.itemCount) {
                            cardList.add(mAdapter.getCard(i).id)
                        }

                        putIntegerArrayList(LoyaltyCardViewActivity.BUNDLE_CARDLIST, cardList)
                    })
                }
            )
        }
    }

    companion object {
        private const val TAG = "Catima"
        const val RESTART_ACTIVITY_INTENT: String = "restart_activity_intent"

        private const val MEDIUM_SCALE_FACTOR_DIP = 460
        const val STATE_SEARCH_QUERY: String = "SEARCH_QUERY"
    }
}
