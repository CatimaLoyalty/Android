package protect.card_locker.widget

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import protect.card_locker.CatimaAppCompatActivity
import protect.card_locker.DBHelper
import protect.card_locker.ListWidget
import protect.card_locker.LoyaltyCardLockerApplication
import protect.card_locker.R
import protect.card_locker.Utils
import protect.card_locker.core.WidgetSettings
import protect.card_locker.core.WidgetSettingsManager
import protect.card_locker.databinding.ActivityWidgetConfigurationBinding
import protect.card_locker.databinding.SortingOptionBinding

class WidgetConfigurationActivity : CatimaAppCompatActivity() {

    private lateinit var binding: ActivityWidgetConfigurationBinding

    private lateinit var settingsManager: WidgetSettingsManager
    private var currentSettings: WidgetSettings = WidgetSettings()
    private lateinit var database: SQLiteDatabase
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    @SuppressLint("VisibleForTests")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout and set the content view using View Binding
        binding = ActivityWidgetConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Utils.applyWindowInsets(binding.getRoot())

        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // If the activity was not launched properly, finish it immediately
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        settingsManager = LoyaltyCardLockerApplication.settingsManager
        database = DBHelper(this).readableDatabase

        lifecycleScope.launch {
            currentSettings = settingsManager.settingsFlow.first()

            // Setup the UI components
            setupGroupSpinner()
            setupStarredFilter()
            setupArchiveFilter()
            setupSortOrderSpinner()
            setupSaveButton()
        }
    }

    private fun setupGroupSpinner() {
        // Perform database operations on a background thread
        lifecycleScope.launch(Dispatchers.IO) {
            val groups = DBHelper.getGroups(database)
            val groupOptions = listOf(getString(R.string.all)) + groups.map { it._id }

            val currentSelection =
                if (currentSettings.group.isNullOrEmpty()) groupOptions.firstOrNull()
                else currentSettings.group

            // Switch back to the main thread to update the UI
            withContext(Dispatchers.Main) {
                val adapter =
                    ArrayAdapter(
                        this@WidgetConfigurationActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        groupOptions
                    )
                binding.groupSpinnerAutocomplete.setAdapter(adapter)
                binding.groupSpinnerAutocomplete.setText(currentSelection, false)

                binding.groupSpinnerAutocomplete.setOnItemClickListener { parent, view, position, id ->
                    // Get the selected string from the adapter using the position
                    val selectedGroup = parent.getItemAtPosition(position).toString()

                    // Update your currentSettings with the new value
                    currentSettings = currentSettings.copy(group = selectedGroup)
                }
            }
        }
    }

    private fun setupStarredFilter() {
        val isFilterEnabled = currentSettings.starFilter != null
        binding.starredFilterCheckbox.isChecked = isFilterEnabled

        binding.starredSwitchContainer.visibility = if (isFilterEnabled) View.VISIBLE else View.GONE
        // When the filter is enabled, the switch is ON for `true` and OFF for `false`
        binding.starredStatusSwitch.isChecked = currentSettings.starFilter == true

        binding.starredFilterCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.starredSwitchContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                // Unchecking the box means show ALL, so set filter to null
                currentSettings = currentSettings.copy(starFilter = null)
            } else {
                // When checking, default to showing Not Starred (false)
                binding.starredStatusSwitch.isChecked = false
                currentSettings = currentSettings.copy(starFilter = false)
            }
        }

        binding.starredStatusSwitch.setOnCheckedChangeListener { _, isChecked ->
            // true for Starred, false for Not Starred
            currentSettings = currentSettings.copy(starFilter = isChecked)
        }
    }

    private fun setupSortOrderSpinner() {
        val items = resources.getStringArray(R.array.sort_types_array)

        updateSortOrderText(currentSettings)

        binding.sortOrderSpinnerAutocomplete.setOnClickListener {

            val sortingOptionBinding = SortingOptionBinding.inflate(
                LayoutInflater.from(this)
            )
            sortingOptionBinding.checkBoxReverse.isChecked =
                currentSettings.sortOrderDirection == DBHelper.LoyaltyCardOrderDirection.Descending
            var currentIndex = currentSettings.sortOrder.ordinal

            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.sort_by)
                .setView(sortingOptionBinding.root)
                .setCancelable(false)
                .setSingleChoiceItems(items, currentIndex) { _, which ->
                    currentIndex = which
                }
                .setPositiveButton(R.string.sort) { dialog, _ ->
                    lifecycleScope.launch {
                        val newSortOrder = DBHelper.LoyaltyCardOrder.entries[currentIndex]
                        val direction = if (sortingOptionBinding.checkBoxReverse.isChecked)
                            DBHelper.LoyaltyCardOrderDirection.Descending
                        else DBHelper.LoyaltyCardOrderDirection.Ascending

                        // Save settings. The flow will automatically update the UI.
                        currentSettings = currentSettings.copy(
                            sortOrder = newSortOrder,
                            sortOrderDirection = direction
                        )
                        updateSortOrderText(currentSettings)

                        dialog.dismiss()
                    }
                }
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun setupArchiveFilter() {
        val isFilterEnabled = currentSettings.archiveFilter != DBHelper.LoyaltyCardArchiveFilter.All
        binding.archiveFilterCheckbox.isChecked = isFilterEnabled

        // Set initial state of the switch based on saved settings
        binding.archiveSwitchContainer.visibility = if (isFilterEnabled) View.VISIBLE else View.GONE
        binding.archiveStatusSwitch.isChecked = currentSettings.archiveFilter == DBHelper.LoyaltyCardArchiveFilter.Archived

        // Checkbox listener to show/hide the switch
        binding.archiveFilterCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.archiveSwitchContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
            // If user unchecks the box, reset the filter to All
            if (!isChecked) {
                currentSettings = currentSettings.copy(archiveFilter = DBHelper.LoyaltyCardArchiveFilter.All)
            } else {
                // When checking, default to Unarchived
                binding.archiveStatusSwitch.isChecked = false
                currentSettings = currentSettings.copy(archiveFilter = DBHelper.LoyaltyCardArchiveFilter.Unarchived)
            }
        }

        // Switch listener to toggle between Archived and Unarchived
        binding.archiveStatusSwitch.setOnCheckedChangeListener { _, isChecked ->
            currentSettings = currentSettings.copy(
                archiveFilter = if (isChecked) DBHelper.LoyaltyCardArchiveFilter.Archived else DBHelper.LoyaltyCardArchiveFilter.Unarchived
            )
        }
    }

    private fun updateSortOrderText(settings: WidgetSettings) {
        val items = resources.getStringArray(R.array.sort_types_array)
        if (settings.sortOrder.ordinal < items.size) {
            val sortOrderText = items[settings.sortOrder.ordinal]
            binding.sortOrderSpinnerAutocomplete.setText(
                String.format("%s %s", sortOrderText, "(${settings.sortOrderDirection.name})")
            )
        }
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            lifecycleScope.launch {
                settingsManager.saveSettings(currentSettings)

                val updateIntent =
                    Intent(this@WidgetConfigurationActivity, ListWidget::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
                    }
                sendBroadcast(updateIntent)

                val resultValue = Intent().apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                setResult(RESULT_OK, resultValue)
                finish()
            }
        }
    }
}