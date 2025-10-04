package protect.card_locker.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import protect.card_locker.DBHelper
import protect.card_locker.LoyaltyCardLockerApplication
import protect.card_locker.R
import protect.card_locker.core.WidgetSettingsManager

@RunWith(AndroidJUnit4::class)
class WidgetConfigurationActivityTest {

    private val testContext: Context = ApplicationProvider.getApplicationContext()
    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var settingsManager: WidgetSettingsManager

    private lateinit var scenario: ActivityScenario<WidgetConfigurationActivity>

    // Use a temporary folder for the test DataStore
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    // This setup runs before each test
    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        // Create a fresh DataStore for each test
        testDataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(UnconfinedTestDispatcher() + Job()),
            produceFile = { temporaryFolder.newFile("test_widget_settings.preferences_pb") }
        )
        settingsManager = WidgetSettingsManager(testDataStore)
        val app = ApplicationProvider.getApplicationContext<LoyaltyCardLockerApplication>()
        app.setTestSettingsManager(settingsManager)

        scenario = ActivityScenario.launchActivityForResult(
            Intent(ApplicationProvider.getApplicationContext(), WidgetConfigurationActivity::class.java)
                .apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 12345)
                }
        )
    }

    @After
    fun tearDown() {
        // Clean up the scenario
        scenario.close()
    }

    @Test
    fun initialState_reflectsDefaultSettings() = runTest {
        // ARRANGE: No settings saved yet

        // ACT: Activity is launched by the rule

        // ASSERT: UI shows the default values
        onView(withId(R.id.group_spinner_autocomplete)).check(
            matches(
                withText(
                    testContext.getString(
                        R.string.all
                    )
                )
            )
        )
        onView(withId(R.id.starred_filter_checkbox)).check(matches(not(isChecked())))
        onView(withId(R.id.archive_filter_checkbox)).check(matches(not(isChecked())))
        onView(withId(R.id.starred_switch_container)).check(matches(not(isDisplayed())))
        onView(withId(R.id.archive_switch_container)).check(matches(not(isDisplayed())))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun changeSettingsAndSave_persistsCorrectlyInDataStore() = runTest {

        onView(withId(R.id.starred_filter_checkbox)).perform(click())
        onView(withId(R.id.starred_status_switch)).perform(click())
        onView(withId(R.id.archive_filter_checkbox)).perform(click())
        onView(withId(R.id.save_button)).perform(click())

        // Wait for activity to finish
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        val expectedArchiveFilter = DBHelper.LoyaltyCardArchiveFilter.Unarchived

        val savedSettings = withTimeout(5_000) {
            settingsManager.settingsFlow
                .onEach { println("Observed: $it") }
                .filter { settings ->
                    settings.starFilter == true &&
                            settings.archiveFilter == expectedArchiveFilter &&
                            settings.group == null
                }
                .first()
        }

        assertThat(savedSettings.starFilter).isTrue()
        assertThat(savedSettings.archiveFilter).isEqualTo(DBHelper.LoyaltyCardArchiveFilter.Unarchived)
        assertThat(savedSettings.group).isEqualTo(null)

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
    }


    @Test
    fun checkboxToggle_correctlyChangesSwitchVisibility() {
        // ARRANGE: Activity is open

        // ACT & ASSERT 1: Check initial state
        onView(withId(R.id.archive_switch_container)).check(matches(not(isDisplayed())))

        // ACT & ASSERT 2: Click checkbox to show the switch
        onView(withId(R.id.archive_filter_checkbox)).perform(click())
        onView(withId(R.id.archive_switch_container)).check(matches(isDisplayed()))

        // ACT & ASSERT 3: Click checkbox again to hide it
        onView(withId(R.id.archive_filter_checkbox)).perform(click())
        onView(withId(R.id.archive_switch_container)).check(matches(not(isDisplayed())))
    }
}