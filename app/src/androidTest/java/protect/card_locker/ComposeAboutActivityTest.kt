package protect.card_locker

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

class ComposeAboutActivityTest {
    @get:Rule
    val rule: ComposeContentTestRule = createComposeRule()

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