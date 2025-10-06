package protect.card_locker

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ImportExportActivityTest {

    private fun registerIntentHandler(handler: String) {
        // Add something that will 'handle' the given intent type
        val packageManager = RuntimeEnvironment.application.packageManager

        val info = ResolveInfo().apply {
            isDefault = true
            activityInfo = ActivityInfo().apply {
                applicationInfo = ApplicationInfo().apply {
                    packageName = "does.not.matter"
                }
                name = "DoesNotMatter"
                exported = true
            }
        }

        val intent = Intent(handler)

        if (handler == Intent.ACTION_GET_CONTENT) {
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
        }

        shadowOf(packageManager).addResolveInfoForIntent(intent, info)
    }

    private fun checkVisibility(
        activity: Activity,
        state: Int,
        divider: Int,
        title: Int,
        message: Int,
        button: Int
    ) {
        val dividerView = activity.findViewById<View>(divider)
        val titleView = activity.findViewById<View>(title)
        val messageView = activity.findViewById<View>(message)
        val buttonView = activity.findViewById<View>(button)

        assertEquals(state, dividerView.visibility)
        assertEquals(state, titleView.visibility)
        assertEquals(state, messageView.visibility)
        assertEquals(state, buttonView.visibility)
    }

    @Test
    fun testAllOptionsAvailable() {
        registerIntentHandler(Intent.ACTION_PICK)
        registerIntentHandler(Intent.ACTION_GET_CONTENT)

        val activity = Robolectric.setupActivity(ImportExportActivity::class.java)

        checkVisibility(
            activity, View.VISIBLE, R.id.dividerImportFilesystem,
            R.id.importOptionFilesystemTitle, R.id.importOptionFilesystemExplanation,
            R.id.importOptionFilesystemButton
        )
    }
}