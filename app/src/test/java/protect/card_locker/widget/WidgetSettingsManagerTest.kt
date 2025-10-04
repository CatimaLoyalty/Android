package protect.card_locker.widget

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import protect.card_locker.DBHelper
import protect.card_locker.core.WidgetSettings
import protect.card_locker.core.WidgetSettingsManager


@OptIn(ExperimentalCoroutinesApi::class)
class WidgetSettingsManagerTest {
    // Use a temporary folder to create a test DataStore file
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var manager: WidgetSettingsManager

    @Before
    fun setup() {
        // Set the main dispatcher for coroutines
        Dispatchers.setMain(testDispatcher)

        // Use PreferenceDataStoreFactory to create a test instance of DataStore
        testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { temporaryFolder.newFile("test_widget_settings.preferences_pb") }
        )

        manager = WidgetSettingsManager(testDataStore)
    }

    @After
    fun tearDown() {
        // Reset the main dispatcher
        Dispatchers.resetMain()
    }

    @Test
    fun `settingsFlow emits default settings when DataStore is empty`() = runTest {
        // Action: Collect from the flow without saving anything
        val defaultSettings = manager.settingsFlow.first()

        // Assert: The emitted values match the defaults in the data class
        assertThat(defaultSettings.group).isNull()
        assertThat(defaultSettings.starFilter).isNull()
        assertThat(defaultSettings.archiveFilter).isEqualTo(DBHelper.LoyaltyCardArchiveFilter.All)
        assertThat(defaultSettings.sortOrder).isEqualTo(DBHelper.LoyaltyCardOrder.Alpha)
        assertThat(defaultSettings.sortOrderDirection).isEqualTo(DBHelper.LoyaltyCardOrderDirection.Ascending)
    }

    @Test
    fun `saveSettings correctly persists all values`() = runTest {
        val newSettings = WidgetSettings(
            group = "Work",
            starFilter = true, // Starred only
            archiveFilter = DBHelper.LoyaltyCardArchiveFilter.Archived,
            sortOrder = DBHelper.LoyaltyCardOrder.Alpha,
            sortOrderDirection = DBHelper.LoyaltyCardOrderDirection.Descending
        )

        manager.settingsFlow.test {
            // Skip the initial default emission
            skipItems(1)

            // Action: Save the new settings
            manager.saveSettings(newSettings)

            // Assert: The flow emits the settings we just saved
            val emitted = awaitItem()
            assertThat(emitted).isEqualTo(newSettings)
        }
    }

    @Test
    fun `saveSettings with null group removes the group key`() = runTest {
        val settingsWithGroup = WidgetSettings(group = "Personal")
        val settingsWithoutGroup = WidgetSettings(group = null)

        manager.settingsFlow.test {
            skipItems(1)

            // Action 1: Save settings with a group
            manager.saveSettings(settingsWithGroup)
            assertThat(awaitItem().group).isEqualTo("Personal")

            // Action 2: Save settings with a null group
            manager.saveSettings(settingsWithoutGroup)

            // Assert: The flow emits settings with a null group
            val finalSettings = awaitItem()
            assertThat(finalSettings.group).isNull()
        }
    }

    @Test
    fun `saveSettings with null starFilter removes the starFilter key`() = runTest {
        val settingsWithStarFilter = WidgetSettings(starFilter = false) // Not Starred
        val settingsWithoutStarFilter = WidgetSettings(starFilter = null) // All

        manager.settingsFlow.test {
            skipItems(1)

            // Action 1: Save settings with a star filter
            manager.saveSettings(settingsWithStarFilter)
            assertThat(awaitItem().starFilter).isFalse()

            // Action 2: Save settings with a null star filter
            manager.saveSettings(settingsWithoutStarFilter)

            // Assert: The flow emits settings with a null star filter
            val finalSettings = awaitItem()
            assertThat(finalSettings.starFilter).isNull()
        }
    }

    @Test
    fun `settingsFlow safely handles invalid enum values and returns default`() = runTest {
        // Manually write a bad value to the DataStore
        testDataStore.edit { preferences ->
            preferences[WidgetSettingsManager.KEY_ARCHIVE_FILTER] = "INVALID_ENUM_VALUE"
        }

        // Action: Collect from the flow
        val settings = manager.settingsFlow.first()

        // Assert: The manager fell back to the default value instead of crashing
        assertThat(settings.archiveFilter).isEqualTo(DBHelper.LoyaltyCardArchiveFilter.All)
    }
}