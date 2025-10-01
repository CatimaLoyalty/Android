package protect.card_locker

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController
import protect.card_locker.compose.AboutScreenRoot

@RunWith(AndroidJUnit4::class)
class AboutActivityTest {
    private lateinit var context: Context
    private lateinit var activityController: ActivityController<AboutActivity>
    private lateinit var activity: AboutActivity


    @Before
    fun setUp() {
        // Get the application context
        context = ApplicationProvider.getApplicationContext()

        // Build the activity using Robolectric but don't create/start it yet
        activityController = Robolectric.buildActivity(AboutActivity::class.java)
        activity = activityController.get()
    }

    @get:Rule
    val composableTestRule = createComposeRule()

    @Test
    fun testDisplayDonateOptionWhenTrueInConfig() {
        with(composableTestRule) {
            setContent {
                AboutScreenRoot(
                    title = "Test",
                    showDonate = true,
                    showRateOnGooglePlay = false,
                )
            }

            onNodeWithText(context.getString(R.string.rate_this_app)).assertDoesNotExist()
            onNodeWithText(context.getString(R.string.donate)).assertExists()
        }
    }

    @Test
    fun testDisplayRateAppOptionWhenTrueInConfig() {
        with(composableTestRule) {
            setContent {
                AboutScreenRoot(
                    title = "Test",
                    showDonate = false,
                    showRateOnGooglePlay = true,
                )
            }

            onNodeWithText(context.getString(R.string.rate_this_app)).assertExists()
            onNodeWithText(context.getString(R.string.donate)).assertDoesNotExist()
        }
    }

    @Test
    fun testActivityDestruction() {
        activityController.create().start().resume()

        // Verify a view exists before destruction
        composableTestRule.onNodeWithTag(context.getString(R.string.credits))

        activityController.pause().stop().destroy()

        // Verify activity was destroyed
        assertTrue(activity.isDestroyed)
    }
}