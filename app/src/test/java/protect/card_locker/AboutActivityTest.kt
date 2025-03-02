package protect.card_locker

import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowActivity
import org.robolectric.shadows.ShadowLog
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
class AboutActivityTest {
    private lateinit var activityController: org.robolectric.android.controller.ActivityController<AboutActivity>
    private lateinit var activity: AboutActivity
    private lateinit var shadowActivity: ShadowActivity

    @Before
    fun setUp() {
        ShadowLog.stream = System.out
        activityController = Robolectric.buildActivity(AboutActivity::class.java)
        activity = activityController.get()
        shadowActivity = shadowOf(activity)
    }

    @Test
    fun testActivityCreation() {
        activityController.create().start().resume()

        // Verify activity title is set correctly
        assertEquals(activity.title.toString(),
            activity.getString(R.string.about_title_fmt, activity.getString(R.string.app_name)))

        // Check key elements are initialized
        assertNotNull(activity.findViewById(R.id.toolbar))
        assertNotNull(activity.findViewById(R.id.credits_sub))
        assertNotNull(activity.findViewById(R.id.version_history_sub))
    }

    @Test
    fun testDisplayOptionsBasedOnConfig() {
        activityController.create().start().resume()

        // Test Google Play rate button visibility based on BuildConfig
        val rateButton = activity.findViewById<View>(R.id.rate)
        assertEquals(BuildConfig.showRateOnGooglePlay, rateButton.isVisible)

        // Test donate button visibility based on BuildConfig
        val donateButton = activity.findViewById<View>(R.id.donate)
        assertEquals(BuildConfig.showDonate, donateButton.isVisible)
    }

    @Test
    fun testClickListeners() {
        activityController.create().start().resume()

        // Test clicking on a link that opens external browser
        val repoButton = activity.findViewById<View>(R.id.repo)
        repoButton.performClick()

        val startedIntent = shadowActivity.nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, startedIntent.action)
        assertEquals(Uri.parse("https://github.com/CatimaLoyalty/Android/"),
            startedIntent.data)
    }

    @Test
    fun testActivityDestruction() {
        activityController.create().start().resume()

        // Verify a view exists before destruction
        assertNotNull(activity.findViewById(R.id.credits_sub))

        activityController.pause().stop().destroy()

        // Verify activity was destroyed
        assertTrue(activity.isDestroyed)
    }

    @Test
    fun testDialogContentMethods() {
        activityController.create().start().resume()

        // Use reflection to test private methods
        try {
            val showCreditsMethod: Method = AboutActivity::class.java.getDeclaredMethod("showCredits")
            showCreditsMethod.isAccessible = true
            showCreditsMethod.invoke(activity) // Should not throw exception

            val showHistoryMethod: Method = AboutActivity::class.java.getDeclaredMethod("showHistory", View::class.java)
            showHistoryMethod.isAccessible = true
            showHistoryMethod.invoke(activity, activity.findViewById(R.id.version_history)) // Should not throw exception
        } catch (e: Exception) {
            fail("Exception when calling dialog methods: ${e.message}")
        }
    }

    @Test
    fun testExternalBrowserWithDifferentURLs() {
        activityController.create().start().resume()

        try {
            // Get access to the private method
            val openExternalBrowserMethod: Method = AboutActivity::class.java.getDeclaredMethod("openExternalBrowser", View::class.java)
            openExternalBrowserMethod.isAccessible = true

            // Create test URLs
            val testUrls = arrayOf(
                "https://hosted.weblate.org/engage/catima/",
                "https://github.com/CatimaLoyalty/Android/blob/main/LICENSE",
                "https://catima.app/privacy-policy/",
                "https://github.com/CatimaLoyalty/Android/issues"
            )

            for (url in testUrls) {
                // Create a View with the URL as tag
                val testView = View(activity)
                testView.tag = url

                // Call the method directly
                openExternalBrowserMethod.invoke(activity, testView)

                // Verify the intent
                val intent = shadowActivity.nextStartedActivity
                assertNotNull("No intent launched for URL: $url", intent)
                assertEquals(Intent.ACTION_VIEW, intent.action)
                assertEquals(Uri.parse(url), intent.data)
            }
        } catch (e: Exception) {
            fail("Exception during reflection: ${e.message}")
        }
    }

    @Test
    fun testButtonVisibilityBasedOnBuildConfig() {
        activityController.create().start().resume()

        // Get the current values from BuildConfig
        val showRateOnGooglePlay = BuildConfig.showRateOnGooglePlay
        val showDonate = BuildConfig.showDonate

        // Test that the visibility matches the BuildConfig values
        assertEquals(showRateOnGooglePlay, activity.findViewById<View>(R.id.rate).isVisible)
        assertEquals(showDonate, activity.findViewById<View>(R.id.donate).isVisible)
    }

    @Test
    fun testAboutScreenTextContent() {
        activityController.create().start().resume()

        // Verify that text fields contain the expected content
        val creditsSub = activity.findViewById<TextView>(R.id.credits_sub)
        assertNotNull(creditsSub.text)
        assertFalse(creditsSub.text.toString().isEmpty())

        val versionHistorySub = activity.findViewById<TextView>(R.id.version_history_sub)
        assertNotNull(versionHistorySub.text)
        assertFalse(versionHistorySub.text.toString().isEmpty())
    }
}
