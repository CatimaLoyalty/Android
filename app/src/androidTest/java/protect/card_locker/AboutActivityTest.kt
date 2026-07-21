package protect.card_locker

import android.app.Instrumentation
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import protect.card_locker.compose.theme.CatimaTheme

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AboutActivityTest {
    @get:Rule
    private val rule: ComposeContentTestRule = createComposeRule()

    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()

    private val content: AboutContent = AboutContent(instrumentation.targetContext)

    @Test
    fun testInitialState(): Unit = runComposeUiTest {
        setContent {
            AboutScreenContent(content = content)
        }

        onNodeWithTag("topbar_catima").assertIsDisplayed()

        onNodeWithTag("card_version_history").assertIsDisplayed()
        onNodeWithText(content.versionHistory).assertIsDisplayed()

        onNodeWithTag("card_credits").assertIsDisplayed()
        onNodeWithText(content.copyrightShort).assertIsDisplayed()

        onNodeWithTag("card_translate").assertIsDisplayed()
        onNodeWithTag("card_license").assertIsDisplayed()

        // We might be off the screen so start scrolling
        onNodeWithTag("card_source_github").performScrollTo().assertIsDisplayed()
        onNodeWithTag("card_privacy_policy").performScrollTo().assertIsDisplayed()
        onNodeWithTag("card_donate").performScrollTo().assertIsDisplayed()
        // Dont scroll to this, since its not displayed
        onNodeWithTag("card_rate_google").assertIsNotDisplayed()
        onNodeWithTag("card_report_error").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun testDonateAndGoogleCardVisible(): Unit = runComposeUiTest {
        setContent {
            CatimaTheme {
                AboutScreenContent(
                    content = content,
                    showDonate = true,
                    showRateOnGooglePlay = true,
                )
            }
        }

        onNodeWithTag("card_donate").performScrollTo().assertIsDisplayed()
        onNodeWithTag("card_rate_google").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun testDonateAndGoogleCardHidden(): Unit = runComposeUiTest {
        setContent {
            CatimaTheme {
                AboutScreenContent(
                    content = content,
                    showDonate = false,
                    showRateOnGooglePlay = false,
                )
            }
        }

        onNodeWithTag("card_privacy_policy").performScrollTo().assertIsDisplayed()
        onNodeWithTag("card_donate").assertIsNotDisplayed()
        onNodeWithTag("card_rate_google").assertIsNotDisplayed()
        onNodeWithTag("card_report_error").performScrollTo().assertIsDisplayed()
    }
}