package protect.card_locker

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AboutActivityIntegrationTest {
    private lateinit var context: Context
    @get:Rule
    val composableTestRule = createAndroidComposeRule<AboutActivity>()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testActivityCreation() {
        val title = context.getString(R.string.about_title_fmt, context.getString(R.string.app_name))

        // Ensure the UI is ready before checking the nodes
        composableTestRule.waitForIdle()

        with(composableTestRule) {
            // Check title exists
            onNodeWithText(title).assertExists()

            // Check key elements are initialized
            onNodeWithText(context.getString(R.string.credits)).assertExists()
            onNodeWithText(context.getString(R.string.version_history)).assertExists()
            onNodeWithText(context.getString(R.string.help_translate_this_app)).assertExists()
            onNodeWithText(context.getString(R.string.license)).assertExists()
            onNodeWithText(context.getString(R.string.source_repository)).assertExists()
            onNodeWithText(context.getString(R.string.privacy_policy)).assertExists()
            onNodeWithText(context.getString(R.string.report_error)).assertExists()
        }
    }


    @Test
    fun testDialogContentMethods() {
        // Use reflection to test private methods
        with(composableTestRule) {
            onNodeWithTag(context.getString(R.string.license))
                .performClick()

            onNodeWithText(context.getString(R.string.ok))
                .assertIsDisplayed()
                .performClick()

            onNodeWithText(context.getString(R.string.ok))
                .assertIsNotDisplayed()
        }
    }

    @Test
    fun testClickListeners() {
        // need to catch the Intent. This requires Espresso-Intents
        Intents.init()

        with(composableTestRule) {
            onNodeWithTag(context.getString(R.string.source_repository))
                .performClick()

            waitForIdle()
        }

        intended(
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(AppURLs.REPOSITORY_SOURCE)
            )
        )

        Intents.release()
    }

    @Test
    fun testOpenBrowserOnVersionHistoryUrl() {
        Intents.init()

        with(composableTestRule) {
            onNodeWithTag(context.getString(R.string.version_history))
                .performClick()

            onNodeWithText(context.getString(R.string.view_online))
                 .assertIsDisplayed()
                .performClick()

            waitForIdle()
        }

        intended(
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(AppURLs.VERSION_HISTORY)
            )
        )

        Intents.release()
    }

    @Test
    fun testOpenBrowserWhenClickOnHelpTranslateThisApp() {
        Intents.init()

        with(composableTestRule) {
            onNodeWithTag(context.getString(R.string.help_translate_this_app))
                .performClick()

            waitForIdle()
        }

        intended(
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(AppURLs.HELP_TRANSLATE_APP)
            )
        )

        Intents.release()
    }

    @Test
    fun testOpenBrowserWhenClickOnLicense() {
        Intents.init()

        with(composableTestRule) {
            onNodeWithTag(context.getString(R.string.license))
                .performClick()

            onNodeWithText(context.getString(R.string.view_online))
                .assertIsDisplayed()
                .performClick()
        }

        intended(
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(AppURLs.LICENSE)
            )
        )

        Intents.release()
    }

    @Test
    fun testOpenBrowserWhenClickOnSourceRepository() {
        Intents.init()

        composableTestRule
            .onNodeWithTag(context.getString(R.string.source_repository))
            .performClick()

        intended(
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(AppURLs.REPOSITORY_SOURCE)
            )
        )

        Intents.release()
    }

    @Test
    fun testOpenBrowserWhenClickOnPrivacyPolicy() {
        Intents.init()

        with(composableTestRule) {
            onNodeWithTag(context.getString(R.string.privacy_policy))
                .performClick()

            onNodeWithText(context.getString(R.string.view_online))
                .assertIsDisplayed()
                .performClick()
        }

        intended(
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(AppURLs.PRIVACY_POLICY)
            )
        )

        Intents.release()
    }

    @Test
    fun testOpenBrowserWhenClickOnDonate() {
        Intents.init()

        composableTestRule
            .onNodeWithTag(context.getString(R.string.donate))
            .performClick()

        intended(
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(AppURLs.DONATE)
            )
        )

        Intents.release()
    }

    @Test
    fun testOpenBrowserWhenClickOnReportError() {
        Intents.init()

        composableTestRule
            .onNodeWithTag(context.getString(R.string.report_error))
            .performClick()

        intended(
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(AppURLs.REPORT_ERROR)
            )
        )

        Intents.release()
    }
}