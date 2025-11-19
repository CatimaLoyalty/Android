package protect.card_locker

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowActivity
import org.robolectric.shadows.ShadowLog

@RunWith(AndroidJUnit4::class)
class AboutActivityTest {
    @get:Rule
    val rule: ComposeContentTestRule = createComposeRule()

    @Before
    fun setUp() {
        ShadowLog.stream = System.out
    }

    @Test
    fun testPasses(): Unit = with(rule) {
        setContent {
            AboutScreenContent(AboutContent(LocalContext.current))
        }

        onNodeWithTag("topbar_catima").assertIsDisplayed()
    }

    @Test
    fun testFails(): Unit = with(rule) {
        setContent {
            AboutScreenContent(AboutContent(LocalContext.current))
        }

        onNodeWithTag("topbar_catima").assertIsNotDisplayed()
    }
}
