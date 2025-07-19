package protect.card_locker.contentprovider

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyArray
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowActivity
import protect.card_locker.AboutActivity
import protect.card_locker.AboutContent
import protect.card_locker.OpenWebLinkHandler
import protect.card_locker.R
import protect.card_locker.compose.CatimaAboutSection
import protect.card_locker.compose.CatimaTopAppBar


@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class AboutActivityUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var activity: AboutActivity
    private lateinit var shadowActivity: ShadowActivity
    private lateinit var mockOpenWebLinkHandler: OpenWebLinkHandler
    private lateinit var mockBackPressedDispatcher: OnBackPressedDispatcher
    private lateinit var mockContext: Context


    @Before
    fun setUp() {
        // Initialize Robolectric activity
        val activityController = Robolectric.buildActivity(AboutActivity::class.java)
        activity = activityController.get()
        shadowActivity = shadowOf(activity)
        val mockPackageManager = mock<PackageManager> {
            on { getPackageInfo(eq("protect.card_locker.contentprovider"), eq(0)) } doReturn PackageInfo().apply { versionName = "1.0.0" }
        }
        val mockResources = mock<Resources> {
            on { getColor(any(), any()) } doReturn 0xFF000000.toInt() // Default color (black)
        }



        // Mock Context and PackageManager for AboutContent
        mockContext = mock {
            on { getString(eq(R.string.app_name)) } doReturn "Catima"
            on { getString(eq(R.string.about_title_fmt), eq("Catima")) } doReturn "About Catima"
            on { getString(eq(R.string.debug_version_fmt)) } doReturn "Version %s"
            on { getString(eq(R.string.debug_version_fmt), eq("1.0.0")) } doReturn "Version 1.0.0"
            on { getString(eq(R.string.app_copyright_fmt), eq(2025)) } doReturn "Copyright © 2025"
            on { getString(eq(R.string.app_copyright_short)) } doReturn "Copyright 2023"
            on { getString(eq(R.string.app_contributors), anyArray<String>()) } doReturn "Contributors: <br/>Test Contributor"
            on { getString(eq(R.string.app_libraries), anyArray<String>()) } doReturn "Libraries: <br/>Test Library"
            on { getString(eq(R.string.app_resources), anyArray<String>()) } doReturn "Resources: <br/>Test Asset"
            on { getString(eq(R.string.app_copyright_old)) } doReturn "Old Copyright"
            on { getString(eq(R.string.version_history)) } doReturn "Version history"
            on { getString(eq(R.string.credits)) } doReturn "Credits"
            on { getString(eq(R.string.help_translate_this_app)) } doReturn "Help translate this app"
            on { getString(eq(R.string.license)) } doReturn "License"
            on { getString(eq(R.string.source_repository)) } doReturn "Source repository"
            on { getString(eq(R.string.privacy_policy)) } doReturn "Privacy policy"
            on { getString(eq(R.string.donate)) } doReturn "Donate"
            on { getString(eq(R.string.rate_this_app)) } doReturn "Rate this app"
            on { getString(eq(R.string.report_error)) } doReturn "Report error"
            on { getString(eq(R.string.translate_platform)) } doReturn "Weblate"
            on { getString(eq(R.string.app_license)) } doReturn "MIT"
            on { getString(eq(R.string.on_github)) } doReturn "GitHub"
            on { getString(eq(R.string.and_data_usage)) } doReturn "and data usage"
            on { getString(eq(R.string.ok)) } doReturn "OK"
            on { getString(eq(R.string.view_online)) } doReturn "View online"
            on { getPackageName() } doReturn "com.example.catima"
            on { getPackageName() } doReturn "protect.card_locker.contentprovider"
            on { packageManager } doReturn mockPackageManager
            on {resources} doReturn mockResources
            }

        activity.content = MockAboutContent(mockContext)
        mockOpenWebLinkHandler = mock()
        mockBackPressedDispatcher = mock()
    }

    class MockAboutContent(context: Context) : AboutContent(context) {
        override fun getContributorsHtml(): String = "<br/>Test Contributor"
        override fun getHistoryHtml(): String = "<p>Version 1.0</p>"
        override fun getLicenseHtml(): String = "<p>MIT License</p>"
        override fun getPrivacyHtml(): String = "<p>Privacy Policy</p>"
        override fun getThirdPartyLibrariesHtml(): String = "<br/>Test Library"
        override fun getUsedThirdPartyAssetsHtml(): String = "<br/>Test Asset"
        override fun getCurrentYear(): Int = 2025
    }
    // Mock OpenWebLinkHandler to verify URL opening
    class MockOpenWebLinkHandler : OpenWebLinkHandler() {
        override fun openBrowser(activity: Activity?, url: String?) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            activity?.startActivity(intent)
        }
    }

    @Test
    fun testTopAppBarTitleAndBackButton() {
        composeTestRule.setContent {
           CatimaTopAppBar(
                title = "About Catima",
                onBackPressedDispatcher = mockBackPressedDispatcher
            )
        }

        // Verify title
        composeTestRule.onNodeWithText("About Catima").assertIsDisplayed()

        // Verify back button and click
        composeTestRule.onNodeWithContentDescription("Back")
            .assertIsDisplayed()
            .performClick()

        // Verify back button triggers onBackPressed
        verify(mockBackPressedDispatcher).onBackPressed()
    }

    @Test
    fun testTopAppBarNoBackButtonWhenDispatcherNull() {
        composeTestRule.setContent {
            CatimaTopAppBar(
                title = "About Catima",
                onBackPressedDispatcher = null
            )
        }

        // Verify title
        composeTestRule.onNodeWithText("About Catima").assertIsDisplayed()

        // Verify back button is not displayed
        composeTestRule.onNodeWithContentDescription("Back").assertDoesNotExist()
    }

    @Test
    fun testCatimaAboutSectionTitleAndMessage() {
        composeTestRule.setContent {
           CatimaAboutSection(
                title = "Test Section",
                message = "Test Message",
                onClickUrl = null,
                onClickDialogText = null
            )
        }

        // Verify title and message
        composeTestRule.onNodeWithText("Test Section").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Message").assertIsDisplayed()

        // Verify chevron icon
        composeTestRule.onNodeWithText(">").assertIsDisplayed()
    }

    @Test
    fun testCatimaAboutSectionUrlClick() {
        composeTestRule.setContent {
            CatimaAboutSection(
                title = "Source repository",
                message = "GitHub",
                onClickUrl = "https://github.com/CatimaLoyalty/Android/",
                onClickDialogText = null
            )
        }

        // Click the section
        composeTestRule.onNodeWithText("Source repository")
            .assertIsDisplayed()
            .performClick()

        // Verify intent
        val intent = shadowActivity.nextStartedActivity
        assertNotNull("No intent launched", intent)
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(Uri.parse("https://github.com/CatimaLoyalty/Android/"), intent.data)
    }

    @Test
    fun testCatimaAboutSectionDialogDisplay() {
        val dialogText = AnnotatedString("Test Dialog Content")
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides mockContext) {  }
            CatimaAboutSection(
                title = "Credits",
                message = "Copyright 2023",
                onClickUrl = null,
                onClickDialogText = dialogText
            )
        }

        // Click the section to open dialog
        composeTestRule.onNodeWithText("Credits")
            .assertIsDisplayed()
            .performClick()

        // Verify dialog content
      //  composeTestRule.onNodeWithText("Credits").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Dialog Content").assertIsDisplayed()
        composeTestRule.onNodeWithText("OK").assertIsDisplayed()

        // Click OK to dismiss
        composeTestRule.onNodeWithText("OK").performClick()

        // Verify dialog is dismissed
        composeTestRule.onNodeWithText("Test Dialog Content").assertDoesNotExist()
    }

    @Test
    fun testCatimaAboutSectionDialogWithViewOnline() {
        val dialogText = AnnotatedString("Test Dialog Content")
        composeTestRule.setContent {
            CatimaAboutSection(
                title = "License",
                message = "MIT",
                onClickUrl = "https://github.com/CatimaLoyalty/Android/blob/main/LICENSE",
                onClickDialogText = dialogText
            )
        }

        // Click the section to open dialog
        composeTestRule.onNodeWithText("License")
            .assertIsDisplayed()
            .performClick()

        // Verify dialog content
//        composeTestRule.onNodeWithText("License").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Dialog Content").assertIsDisplayed()
        composeTestRule.onNodeWithText("View online").assertIsDisplayed()

        // Click View online
        composeTestRule.onNodeWithText("View online").performClick()

        // Verify intent
        val intent = shadowActivity.nextStartedActivity
        assertNotNull("No intent launched", intent)
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(Uri.parse("https://github.com/CatimaLoyalty/Android/blob/main/LICENSE"), intent.data)
    }

    @Test
    fun testAllSectionsDisplayed() {
        composeTestRule.setContent {
            activity.ScreenContent(
                showDonate = true,
                showRateOnGooglePlay = true
            )
        }

        // List of expected section titles
        val sections = listOf(
            "Version history",
            "Credits",
            "Help translate this app",
            "License",
            "Source repository",
            "Privacy policy",
            "Donate",
          //  "Rate this app",
            "Report error"
        )

        sections.forEach { section ->
            composeTestRule.onNodeWithText(section)
                .assertIsDisplayed()
                //.performScrollTo() // Ensure section is visible
        }
    }

    @Test
    fun testSectionSubtitles() {
        composeTestRule.setContent {
            activity.ScreenContent(
                showDonate = true,
                showRateOnGooglePlay = true
            )
        }

        // Verify subtitles for specific sections
        composeTestRule.onNodeWithText("Version History Subtitle").assertIsDisplayed()
        composeTestRule.onNodeWithText("Copyright 2023").assertIsDisplayed()
        composeTestRule.onNodeWithText("Weblate").assertIsDisplayed()
        composeTestRule.onNodeWithText("MIT").assertIsDisplayed()
        composeTestRule.onNodeWithText("GitHub").assertIsDisplayed()
        composeTestRule.onNodeWithText("and data usage").assertIsDisplayed()
    }

    @Test
    fun testDonateSectionVisibility() {
        composeTestRule.setContent {
            activity.ScreenContent(
                showDonate = false,
                showRateOnGooglePlay = true
            )
        }

        // Verify Donate section is not displayed
        composeTestRule.onNodeWithText("Donate").assertDoesNotExist()

        // Verify other sections are still present
        composeTestRule.onNodeWithText("Rate this app").assertIsDisplayed()
        composeTestRule.onNodeWithText("Version history").assertIsDisplayed()
    }

    @Test
    fun testRateOnGooglePlaySectionVisibility() {
        composeTestRule.setContent {
            activity.ScreenContent(
                showDonate = true,
                showRateOnGooglePlay = false
            )
        }


        composeTestRule.onNodeWithText("Donate").assertIsDisplayed()
        composeTestRule.onNodeWithText("Version history").assertIsDisplayed()
    }

}