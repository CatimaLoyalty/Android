package protect.card_locker.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import protect.card_locker.DBHelper
import protect.card_locker.R
import protect.card_locker.core.WidgetSettingsManager

@RunWith(AndroidJUnit4::class)
class DataStoreMigrationTest {

    private val testContext: Context = ApplicationProvider.getApplicationContext()
    private val testSharedPrefsName = "test_old_prefs"
    private val testDataStoreFileName = "test_widget_settings.preferences_pb"
    private lateinit var oldKeySortOrder: String
    private lateinit var oldKeySortDirection: String
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    // --- CHANGE 1: Create a single dispatcher for the test class ---
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var testDataStore: DataStore<Preferences>

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        oldKeySortOrder = context.getString(R.string.sharedpreference_sort_order)
        oldKeySortDirection = context.getString(R.string.sharedpreference_sort_direction)
        // ✅ CORRECT: Create the DataStore instance using the factory
        testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { temporaryFolder.newFile(testDataStoreFileName) },
            // Pass your migration logic here
            migrations = listOf(
                SharedPreferencesMigration(
                    context = testContext,
                    sharedPreferencesName = testSharedPrefsName,
                    migrate = { sharedPrefs, currentData ->
                        val sort = sharedPrefs.getString(oldKeySortOrder, null)
                        val sortDirection = sharedPrefs.getString(oldKeySortDirection, null)

                        val mutableCurrentData = currentData.toMutablePreferences()

                        if (sort != null) {
                            mutableCurrentData[WidgetSettingsManager.KEY_SORT_ORDER] = sort
                        }
                        if (sortDirection != null) {
                            // KEY_STAR_FILTER is the correct key from your manager
                            mutableCurrentData[WidgetSettingsManager.KEY_SORT_ORDER_DIRECTION] = sortDirection
                        }

                        mutableCurrentData.toPreferences()
                    }
                )
            )
        )
    }

    @Test
    fun migration_correctlyMapsOldKeysToNewSettings() = runTest(testDispatcher) {
        // ARRANGE: Simulate an existing user by writing to the old SharedPreferences file.
        val sharedPrefs = testContext.getSharedPreferences(testSharedPrefsName, Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putString(oldKeySortOrder, DBHelper.LoyaltyCardOrder.LastUsed.name)
            .putString(oldKeySortDirection, DBHelper.LoyaltyCardOrderDirection.Descending.name) // User wants to see only starred cards
            .commit()

        // ACT: Instantiate the manager and trigger the migration by reading the flow.
        val manager = WidgetSettingsManager(testDataStore)
        val migratedSettings = manager.settingsFlow.first()

        // ASSERT: Verify that the data was migrated and mapped correctly.
        assertThat(migratedSettings.sortOrder).isEqualTo(DBHelper.LoyaltyCardOrder.LastUsed)
        assertThat(migratedSettings.sortOrderDirection).isEqualTo(DBHelper.LoyaltyCardOrderDirection.Descending)
        assertThat(migratedSettings.group).isNull() // This key didn't exist, should be default
    }
}