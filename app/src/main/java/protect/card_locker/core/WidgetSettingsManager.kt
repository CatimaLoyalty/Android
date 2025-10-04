package protect.card_locker.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import protect.card_locker.DBHelper
import protect.card_locker.R

// Define the DataStore instance at the top level
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "widget_settings.preferences_pb",
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName = context.getString(R.string.sharedpreference_sort),
                keysToMigrate = mutableSetOf(
                    context.getString(R.string.sharedpreference_sort_order),
                    context.getString(R.string.sharedpreference_sort_direction),
                )
            ) { sharedPrefs, currentData ->
                val oldKeySort = context.getString(R.string.sharedpreference_sort_order)
                val oldKeyDirection = context.getString(R.string.sharedpreference_sort_direction)

                val direction = sharedPrefs.getString(
                    oldKeyDirection,
                    DBHelper.LoyaltyCardOrderDirection.Ascending.name
                )
                    ?: DBHelper.LoyaltyCardOrderDirection.Ascending.name
                val sort = sharedPrefs.getString(oldKeySort, DBHelper.LoyaltyCardOrder.Alpha.name)
                    ?: DBHelper.LoyaltyCardOrder.Alpha.name

                val mutableCurrentData = currentData.toMutablePreferences()
                mutableCurrentData[WidgetSettingsManager.KEY_SORT_ORDER] = sort
                mutableCurrentData[WidgetSettingsManager.KEY_SORT_ORDER_DIRECTION] = direction

                mutableCurrentData.toPreferences()
            }
        )
    }
)

// A data class to hold all our settings together
data class WidgetSettings(
    val group: String? = null, // this will be group._id
    val starFilter: Boolean? = null,
    val sortOrder: DBHelper.LoyaltyCardOrder = DBHelper.LoyaltyCardOrder.Alpha,
    val sortOrderDirection: DBHelper.LoyaltyCardOrderDirection = DBHelper.LoyaltyCardOrderDirection.Ascending,
    val archiveFilter: DBHelper.LoyaltyCardArchiveFilter = DBHelper.LoyaltyCardArchiveFilter.All
)

class WidgetSettingsManager(private val dataStore: DataStore<Preferences>) {

    // Define typed keys for safety
    companion object {
        val KEY_GROUP = stringPreferencesKey("WIDGET_GROUP")
        val KEY_STAR_FILTER = booleanPreferencesKey("WIDGET_STARRED_FILTER")
        val KEY_SORT_ORDER = stringPreferencesKey("WIDGET_SORT_ORDER")
        val KEY_SORT_ORDER_DIRECTION = stringPreferencesKey("WIDGET_SORT_ORDER_DIRECTION")
        val KEY_ARCHIVE_FILTER = stringPreferencesKey("WIDGET_ARCHIVE_FILTER")
    }


    suspend fun saveSettings(
        group: String,
        isStarredOnly: Boolean,
        sortOrder: DBHelper.LoyaltyCardOrder,
        sortOrderDirection: DBHelper.LoyaltyCardOrderDirection,
        archiveFilter: DBHelper.LoyaltyCardArchiveFilter
    ) {
        dataStore.edit { settings ->
            settings[KEY_GROUP] = group
            settings[KEY_STAR_FILTER] = isStarredOnly
            settings[KEY_SORT_ORDER] = sortOrder.name
            settings[KEY_SORT_ORDER_DIRECTION] = sortOrderDirection.name
            settings[KEY_ARCHIVE_FILTER] = archiveFilter.name
        }
    }

    suspend fun saveSettings(widgetSettings: WidgetSettings) {
        dataStore.edit { settings ->
            if (!widgetSettings.group.isNullOrEmpty()) {
                settings[KEY_GROUP] = widgetSettings.group
            } else {
                settings.remove(KEY_GROUP)
            }
            if (widgetSettings.starFilter != null) {
                settings[KEY_STAR_FILTER] = widgetSettings.starFilter
            } else {
                settings.remove(KEY_STAR_FILTER)
            }

            settings[KEY_SORT_ORDER] = widgetSettings.sortOrder.name
            settings[KEY_SORT_ORDER_DIRECTION] = widgetSettings.sortOrderDirection.name
            settings[KEY_ARCHIVE_FILTER] = widgetSettings.archiveFilter.name
        }
    }

    // Expose a Flow that emits the user's settings whenever they change.
    // The widget will use this to get live updates.
    val settingsFlow: Flow<WidgetSettings> = dataStore.data
        .map { preferences ->
            val group = preferences[KEY_GROUP]
            val starredFilter = preferences[KEY_STAR_FILTER]
            val sortOrderStr = preferences[KEY_SORT_ORDER]
            val sortOrderDirectionStr = preferences[KEY_SORT_ORDER_DIRECTION]
            val archiveFilterStr = preferences[KEY_ARCHIVE_FILTER]

            val sortOrder = if (sortOrderStr.isNullOrEmpty()) DBHelper.LoyaltyCardOrder.Alpha
            else try {
                DBHelper.LoyaltyCardOrder.valueOf(sortOrderStr)
            } catch (_: IllegalArgumentException) {
                DBHelper.LoyaltyCardOrder.Alpha
            }

            val sortDirection =
                if (sortOrderDirectionStr.isNullOrEmpty()) DBHelper.LoyaltyCardOrderDirection.Ascending
                else try {
                    DBHelper.LoyaltyCardOrderDirection.valueOf(sortOrderDirectionStr)
                } catch (_: IllegalArgumentException) {
                    DBHelper.LoyaltyCardOrderDirection.Ascending
                }

            val archiveFilter =
                if (archiveFilterStr.isNullOrEmpty()) DBHelper.LoyaltyCardArchiveFilter.All
                else try {
                    DBHelper.LoyaltyCardArchiveFilter.valueOf(archiveFilterStr)
                } catch (_: IllegalArgumentException) {
                    DBHelper.LoyaltyCardArchiveFilter.All
                }

            WidgetSettings(
                group = group,
                starFilter = starredFilter,
                sortOrder = sortOrder,
                sortOrderDirection = sortDirection,
                archiveFilter = archiveFilter
            )
        }

    val settingsLiveData = settingsFlow.asLiveData()
}